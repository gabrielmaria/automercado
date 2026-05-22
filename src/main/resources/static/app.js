const state = {
    user: null,
    categorias: [],
    produtos: [],
    enderecos: [],
    vendas: [],
    carrinho: [],
    editingProductId: null
};

const dom = {};

document.addEventListener("DOMContentLoaded", () => {
    cacheDom();
    bindEvents();
    loadCart();
    refreshPublicData();
    checkSession();
    renderCart();
});

function cacheDom() {
    dom.notice = document.querySelector("#notice");
    dom.tabs = document.querySelectorAll(".tab");
    dom.views = document.querySelectorAll(".view");
    dom.adminOnly = document.querySelectorAll("[data-admin-only]");
    dom.userBadge = document.querySelector("#userBadge");
    dom.cartCount = document.querySelector("#cartCount");
    dom.productsGrid = document.querySelector("#productsGrid");
    dom.searchInput = document.querySelector("#searchInput");
    dom.categoryFilter = document.querySelector("#categoryFilter");
    dom.refreshCatalog = document.querySelector("#refreshCatalog");
    dom.loginForm = document.querySelector("#loginForm");
    dom.registerForm = document.querySelector("#registerForm");
    dom.authArea = document.querySelector("#authArea");
    dom.profileArea = document.querySelector("#profileArea");
    dom.profileForm = document.querySelector("#profileForm");
    dom.logoutButton = document.querySelector("#logoutButton");
    dom.addressForm = document.querySelector("#addressForm");
    dom.addressesList = document.querySelector("#addressesList");
    dom.salesList = document.querySelector("#salesList");
    dom.checkoutAddress = document.querySelector("#checkoutAddress");
    dom.cartItems = document.querySelector("#cartItems");
    dom.cartTotal = document.querySelector("#cartTotal");
    dom.checkoutButton = document.querySelector("#checkoutButton");
    dom.invoiceBox = document.querySelector("#invoiceBox");
    dom.categoryForm = document.querySelector("#categoryForm");
    dom.productForm = document.querySelector("#productForm");
    dom.cancelProductEdit = document.querySelector("#cancelProductEdit");
    dom.productFormStatus = document.querySelector("#productFormStatus");
    dom.adminProductsList = document.querySelector("#adminProductsList");
    dom.periodSelect = document.querySelector("#periodSelect");
    dom.refreshStats = document.querySelector("#refreshStats");
    dom.revenueValue = document.querySelector("#revenueValue");
    dom.topProducts = document.querySelector("#topProducts");
    dom.lowProducts = document.querySelector("#lowProducts");
    dom.bestClients = document.querySelector("#bestClients");
}

function bindEvents() {
    dom.tabs.forEach((tab) => {
        tab.addEventListener("click", () => showSection(tab.dataset.section));
    });

    dom.searchInput.addEventListener("input", renderProducts);
    dom.categoryFilter.addEventListener("change", renderProducts);
    dom.refreshCatalog.addEventListener("click", refreshPublicData);
    dom.loginForm.addEventListener("submit", login);
    dom.registerForm.addEventListener("submit", register);
    dom.profileForm.addEventListener("submit", updateProfile);
    dom.logoutButton.addEventListener("click", logout);
    dom.addressForm.addEventListener("submit", createAddress);
    dom.checkoutButton.addEventListener("click", checkout);
    dom.categoryForm.addEventListener("submit", createCategory);
    dom.productForm.addEventListener("submit", submitProduct);
    dom.cancelProductEdit.addEventListener("click", resetProductForm);
    dom.refreshStats.addEventListener("click", loadStats);
    dom.periodSelect.addEventListener("change", loadStats);
}

function showSection(id) {
    if (isAdminSection(id) && !isAdmin()) {
        showNotice("Apenas administradores podem aceder a esta área.", true);
        id = "catalogo";
    }

    dom.tabs.forEach((tab) => tab.classList.toggle("is-active", tab.dataset.section === id));
    dom.views.forEach((view) => view.classList.toggle("is-active", view.id === id));
}

async function api(path, options = {}) {
    const request = {
        method: options.method || "GET",
        credentials: "same-origin",
        headers: {
            Accept: "application/json",
            ...(options.headers || {})
        }
    };

    if (options.body !== undefined) {
        request.headers["Content-Type"] = "application/json";
        request.body = JSON.stringify(options.body);
    }

    const response = await fetch(path, request);
    if (response.status === 204) {
        return null;
    }

    const type = response.headers.get("content-type") || "";
    const data = type.includes("application/json")
        ? await response.json().catch(() => null)
        : await response.text().catch(() => "");

    if (!response.ok) {
        throw new Error(readError(data, response.status));
    }

    return data;
}

function readError(data, status) {
    if (data && typeof data === "object") {
        return data.detail || data.message || data.error || `Pedido falhou (${status})`;
    }
    if (typeof data === "string" && data.trim()) {
        return data.trim();
    }
    return `Pedido falhou (${status})`;
}

function showNotice(message, isError = false) {
    dom.notice.textContent = message;
    dom.notice.classList.toggle("is-visible", Boolean(message));
    dom.notice.classList.toggle("is-error", isError);
}

async function refreshPublicData() {
    try {
        const [categorias, produtos] = await Promise.all([
            api("/categorias"),
            api("/produtos")
        ]);
        state.categorias = categorias || [];
        state.produtos = produtos || [];
        renderCategoryOptions();
        renderProducts();
        renderAdminProducts();
        renderCart();
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function checkSession() {
    try {
        state.user = await api("/clientes/perfil");
        await loadPrivateData();
        showNotice(`Sessão iniciada como ${state.user.utilizador}.`);
    } catch (error) {
        state.user = null;
        renderAuthState();
    }
}

async function loadPrivateData() {
    const tasks = [
        loadAddresses(),
        loadSales()
    ];

    if (isAdmin()) {
        tasks.push(loadStats());
    } else {
        renderStatsEmpty();
    }

    await Promise.all(tasks);
    renderAuthState();
}

async function login(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const payload = formToObject(form);

    try {
        state.user = await api("/autenticacao/login", {
            method: "POST",
            body: payload
        });
        form.reset();
        await loadPrivateData();
        showNotice("Login efetuado.");
        showSection("catalogo");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function register(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const payload = formToObject(form);

    try {
        await api("/autenticacao/registar", {
            method: "POST",
            body: payload
        });
        state.user = await api("/autenticacao/login", {
            method: "POST",
            body: {
                utilizador: payload.utilizador,
                password: payload.password
            }
        });
        form.reset();
        await loadPrivateData();
        showNotice("Conta criada e sessão iniciada.");
        showSection("catalogo");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function logout() {
    await fetch("/autenticacao/sair", {
        method: "POST",
        credentials: "same-origin"
    }).catch(() => null);

    state.user = null;
    state.enderecos = [];
    state.vendas = [];
    renderAuthState();
    renderAddresses();
    renderSales();
    renderCheckoutAddresses();
    renderStatsEmpty();
    showNotice("Sessão terminada.");
    showSection("catalogo");
}

async function updateProfile(event) {
    event.preventDefault();
    if (!requireSession()) {
        return;
    }

    const payload = formToObject(event.currentTarget);

    try {
        state.user = await api("/clientes/perfil", {
            method: "PUT",
            body: payload
        });
        fillProfileForm();
        showNotice("Perfil atualizado.");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function loadAddresses() {
    if (!state.user) {
        return;
    }

    state.enderecos = await api("/clientes/enderecos");
    renderAddresses();
    renderCheckoutAddresses();
}

async function createAddress(event) {
    event.preventDefault();
    if (!requireSession()) {
        return;
    }

    const form = event.currentTarget;

    try {
        await api("/clientes/enderecos", {
            method: "POST",
            body: formToObject(form)
        });
        form.reset();
        await loadAddresses();
        showNotice("Endereço adicionado.");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function deleteAddress(id) {
    if (!requireSession()) {
        return;
    }

    try {
        await api(`/clientes/enderecos/${id}`, {
            method: "DELETE"
        });
        await loadAddresses();
        showNotice("Endereço removido.");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function loadSales() {
    if (!state.user) {
        return;
    }

    state.vendas = await api("/vendas");
    renderSales();
}

async function createCategory(event) {
    event.preventDefault();
    if (!requireAdmin()) {
        return;
    }

    const form = event.currentTarget;

    try {
        await api("/categorias", {
            method: "POST",
            body: formToObject(form)
        });
        form.reset();
        await refreshPublicData();
        showNotice("Categoria criada.");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function submitProduct(event) {
    event.preventDefault();
    if (!requireAdmin()) {
        return;
    }

    const form = event.currentTarget;
    const payload = formToObject(form);
    const editing = state.editingProductId !== null;
    payload.categoriaId = Number(payload.categoriaId);
    payload.preco = Number(payload.preco);
    payload.stock = Number(payload.stock);
    payload.ano = optionalNumber(payload.ano);
    payload.quilometragem = optionalNumber(payload.quilometragem);
    delete payload.id;

    try {
        await api(editing ? `/produtos/${state.editingProductId}` : "/produtos", {
            method: editing ? "PUT" : "POST",
            body: payload
        });
        resetProductForm();
        await refreshPublicData();
        showNotice(editing ? "Produto atualizado." : "Produto criado.");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function checkout() {
    if (!requireSession()) {
        return;
    }
    if (!state.carrinho.length) {
        showNotice("O carrinho está vazio.", true);
        return;
    }
    if (!dom.checkoutAddress.value) {
        showNotice("Escolhe um endereço antes de comprar.", true);
        return;
    }

    try {
        const fatura = await api("/vendas", {
            method: "POST",
            body: {
                enderecoId: Number(dom.checkoutAddress.value),
                itens: state.carrinho.map((item) => ({
                    produtoId: item.produtoId,
                    quantidade: item.quantidade
                }))
            }
        });
        state.carrinho = [];
        saveCart();
        renderInvoice(fatura);
        await Promise.all([
            refreshPublicData(),
            loadSales(),
            loadStats()
        ]);
        showNotice("Compra concluída.");
    } catch (error) {
        showNotice(error.message, true);
    }
}

async function loadStats() {
    if (!isAdmin()) {
        renderStatsEmpty();
        return;
    }

    try {
        const periodo = dom.periodSelect.value;
        const [faturacao, maisVendidos, menosVendidos, melhoresClientes] = await Promise.all([
            api(`/estatisticas/faturacao?periodo=${encodeURIComponent(periodo)}`),
            api("/estatisticas/produtos/mais-vendidos?limite=5"),
            api("/estatisticas/produtos/menos-vendidos?limite=5"),
            api("/estatisticas/clientes/melhores?limite=5")
        ]);
        dom.revenueValue.textContent = money(faturacao.valorTotal || 0);
        renderTable(dom.topProducts, ["Produto", "Quantidade"], maisVendidos, [
            (row) => row.produto,
            (row) => row.quantidade
        ]);
        renderTable(dom.lowProducts, ["Produto", "Quantidade"], menosVendidos, [
            (row) => row.produto,
            (row) => row.quantidade
        ]);
        renderTable(dom.bestClients, ["Cliente", "Total"], melhoresClientes, [
            (row) => `${row.nome || ""} ${row.sobrenome || ""}`.trim() || row.utilizador,
            (row) => money(row.valorTotal || 0)
        ]);
    } catch (error) {
        showNotice(error.message, true);
    }
}

function renderCategoryOptions() {
    fillSelect(dom.categoryFilter, [
        { value: "", label: "Todas" },
        ...state.categorias.map((categoria) => ({
            value: String(categoria.id),
            label: categoria.nome
        }))
    ]);

    const productCategory = dom.productForm.elements.categoriaId;
    fillSelect(productCategory, state.categorias.map((categoria) => ({
        value: String(categoria.id),
        label: categoria.nome
    })));
}

function renderProducts() {
    const term = normalize(dom.searchInput.value);
    const categoryId = dom.categoryFilter.value;
    const products = state.produtos.filter((produto) => {
        const text = normalize(`${produto.nome} ${produto.marca} ${produto.modelo}`);
        const matchesTerm = !term || text.includes(term);
        const matchesCategory = !categoryId || String(produto.categoria?.id) === categoryId;
        return matchesTerm && matchesCategory;
    });

    dom.productsGrid.replaceChildren();
    if (!products.length) {
        dom.productsGrid.append(empty("Sem produtos para mostrar."));
        return;
    }

    products.forEach((produto) => {
        const card = document.createElement("article");
        card.className = "product-card";

        const header = document.createElement("header");
        const title = document.createElement("h2");
        title.textContent = produto.nome;
        const price = document.createElement("div");
        price.className = "price";
        price.textContent = money(produto.preco);
        header.append(title, price);

        const resumo = document.createElement("p");
        resumo.textContent = produto.resumodescricao || produto.descricao || "Sem descrição.";

        const meta = document.createElement("div");
        meta.className = "meta-row";
        [
            produto.categoria?.nome,
            produto.marca,
            produto.modelo,
            produto.ano,
            produto.cilindradaPotencia,
            produto.quilometragem ? `${produto.quilometragem} km` : null
        ].filter(Boolean).forEach((value) => {
            const tag = document.createElement("span");
            tag.className = "tag";
            tag.textContent = value;
            meta.append(tag);
        });

        const stock = Number(produto.inventario?.quantidade || 0);
        const stockTag = document.createElement("span");
        stockTag.className = `tag ${stock <= 2 ? "stock-low" : ""}`;
        stockTag.textContent = `Stock: ${stock}`;
        meta.append(stockTag);

        const action = document.createElement("button");
        action.type = "button";
        action.textContent = "Adicionar";
        action.disabled = stock <= 0;
        action.addEventListener("click", () => addToCart(produto.id));

        card.append(header, resumo, meta, action);
        dom.productsGrid.append(card);
    });
}

function renderAdminProducts() {
    dom.adminProductsList.replaceChildren();

    if (!isAdmin()) {
        dom.adminProductsList.append(empty("Apenas administradores podem gerir produtos."));
        return;
    }

    if (!state.produtos.length) {
        dom.adminProductsList.append(empty("Sem produtos à venda."));
        return;
    }

    state.produtos.forEach((produto) => {
        const item = document.createElement("div");
        item.className = "list-item";

        const text = document.createElement("div");
        const strong = document.createElement("strong");
        strong.textContent = `${produto.nome} · ${money(produto.preco)}`;
        const span = document.createElement("span");
        span.textContent = [
            produto.categoria?.nome,
            produto.marca,
            produto.modelo,
            `Stock: ${Number(produto.inventario?.quantidade || 0)}`
        ].filter(Boolean).join(" · ");
        text.append(strong, span);

        const actions = document.createElement("div");
        actions.className = "item-actions";

        const edit = document.createElement("button");
        edit.type = "button";
        edit.className = "secondary";
        edit.textContent = "Editar";
        edit.addEventListener("click", () => startProductEdit(produto.id));

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "danger";
        remove.textContent = "Eliminar";
        remove.addEventListener("click", () => deleteProduct(produto.id));

        actions.append(edit, remove);
        item.append(text, actions);
        dom.adminProductsList.append(item);
    });
}

function renderAuthState() {
    const loggedIn = Boolean(state.user);
    const admin = isAdmin();
    dom.authArea.classList.toggle("hidden", loggedIn);
    dom.profileArea.classList.toggle("hidden", !loggedIn);
    dom.logoutButton.classList.toggle("hidden", !loggedIn);
    dom.adminOnly.forEach((element) => element.classList.toggle("hidden", !admin));
    dom.userBadge.textContent = loggedIn
        ? `${state.user.nome} ${state.user.sobrenome}${admin ? " · Admin" : ""}`
        : "Sessão não iniciada";

    if (loggedIn) {
        fillProfileForm();
    }

    const activeView = document.querySelector(".view.is-active");
    if (!admin && activeView && isAdminSection(activeView.id)) {
        showSection("catalogo");
    }

    if (!admin) {
        resetProductForm();
    }

    renderAdminProducts();
}

function fillProfileForm() {
    dom.profileForm.elements.email.value = state.user.email || "";
    dom.profileForm.elements.nome.value = state.user.nome || "";
    dom.profileForm.elements.sobrenome.value = state.user.sobrenome || "";
}

function startProductEdit(productId) {
    if (!requireAdmin()) {
        return;
    }

    const produto = state.produtos.find((item) => item.id === productId);
    if (!produto) {
        showNotice("Produto não encontrado.", true);
        return;
    }

    state.editingProductId = produto.id;
    const { elements } = dom.productForm;
    elements.id.value = String(produto.id);
    elements.nome.value = produto.nome || "";
    elements.categoriaId.value = String(produto.categoria?.id || "");
    elements.marca.value = produto.marca || "";
    elements.modelo.value = produto.modelo || "";
    elements.preco.value = produto.preco ?? "";
    elements.stock.value = Number(produto.inventario?.quantidade || 0);
    elements.ano.value = produto.ano ?? "";
    elements.quilometragem.value = produto.quilometragem ?? "";
    elements.cilindradaPotencia.value = produto.cilindradaPotencia || "";
    elements.resumodescricao.value = produto.resumodescricao || "";
    elements.descricao.value = produto.descricao || "";

    dom.productForm.querySelector("button[type='submit']").textContent = "Guardar alterações";
    dom.cancelProductEdit.classList.remove("hidden");
    dom.productFormStatus.textContent = `A editar #${produto.id}`;
    dom.productFormStatus.classList.remove("hidden");
    showSection("gestao");
    dom.productForm.scrollIntoView({ behavior: "smooth", block: "start" });
}

function resetProductForm() {
    state.editingProductId = null;
    dom.productForm.reset();
    dom.productForm.elements.id.value = "";
    dom.productForm.querySelector("button[type='submit']").textContent = "Criar produto";
    dom.cancelProductEdit.classList.add("hidden");
    dom.productFormStatus.textContent = "";
    dom.productFormStatus.classList.add("hidden");
}

async function deleteProduct(productId) {
    if (!requireAdmin()) {
        return;
    }

    const produto = state.produtos.find((item) => item.id === productId);
    if (!produto) {
        showNotice("Produto não encontrado.", true);
        return;
    }

    if (!window.confirm(`Eliminar o produto "${produto.nome}"?`)) {
        return;
    }

    try {
        await api(`/produtos/${productId}`, {
            method: "DELETE"
        });
        if (state.editingProductId === productId) {
            resetProductForm();
        }
        await refreshPublicData();
        showNotice("Produto eliminado.");
    } catch (error) {
        showNotice(error.message, true);
    }
}

function renderAddresses() {
    dom.addressesList.replaceChildren();
    if (!state.enderecos.length) {
        dom.addressesList.append(empty("Sem endereços."));
        return;
    }

    state.enderecos.forEach((endereco) => {
        const item = document.createElement("div");
        item.className = "list-item";

        const text = document.createElement("div");
        const strong = document.createElement("strong");
        strong.textContent = endereco.endereco1;
        const span = document.createElement("span");
        span.textContent = [endereco.endereco2, endereco.cidade, endereco.pais]
            .filter(Boolean)
            .join(", ");
        text.append(strong, span);

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "danger";
        remove.textContent = "Remover";
        remove.addEventListener("click", () => deleteAddress(endereco.id));

        item.append(text, remove);
        dom.addressesList.append(item);
    });
}

function renderSales() {
    if (!state.vendas.length) {
        dom.salesList.replaceChildren(empty("Sem vendas."));
        return;
    }

    renderTable(dom.salesList, ["Venda", "Data", "Total"], state.vendas, [
        (row) => `#${row.id}`,
        (row) => formatDate(row.data),
        (row) => money(row.valorTotal || 0)
    ]);
}

function renderCheckoutAddresses() {
    if (!state.enderecos.length) {
        fillSelect(dom.checkoutAddress, [{ value: "", label: "Sem endereços" }]);
        return;
    }

    fillSelect(dom.checkoutAddress, state.enderecos.map((endereco) => ({
        value: String(endereco.id),
        label: `${endereco.endereco1}, ${endereco.cidade}`
    })));
}

function addToCart(produtoId) {
    const product = state.produtos.find((item) => item.id === produtoId);
    const stock = Number(product?.inventario?.quantidade || 0);
    const current = state.carrinho.find((item) => item.produtoId === produtoId);

    if (current) {
        current.quantidade = Math.min(current.quantidade + 1, stock || current.quantidade + 1);
    } else {
        state.carrinho.push({ produtoId, quantidade: 1 });
    }

    saveCart();
    renderCart();
    showNotice("Produto adicionado ao carrinho.");
}

function renderCart() {
    const totalItems = state.carrinho.reduce((sum, item) => sum + item.quantidade, 0);
    dom.cartCount.textContent = String(totalItems);
    dom.cartItems.replaceChildren();

    if (!state.carrinho.length) {
        dom.cartItems.append(empty("Carrinho vazio."));
        dom.cartTotal.textContent = money(0);
        dom.checkoutButton.disabled = true;
        return;
    }

    let total = 0;
    state.carrinho.forEach((item) => {
        const produto = state.produtos.find((candidate) => candidate.id === item.produtoId);
        if (!produto) {
            return;
        }
        const subtotal = produto.preco * item.quantidade;
        total += subtotal;

        const row = document.createElement("div");
        row.className = "list-item";

        const text = document.createElement("div");
        const strong = document.createElement("strong");
        strong.textContent = produto.nome;
        const span = document.createElement("span");
        span.textContent = `${money(produto.preco)} cada · ${money(subtotal)}`;
        text.append(strong, span);

        const controls = document.createElement("div");
        controls.className = "quantity-control";
        const quantity = document.createElement("input");
        quantity.type = "number";
        quantity.min = "1";
        quantity.step = "1";
        quantity.value = String(item.quantidade);
        quantity.addEventListener("change", () => updateCartQuantity(item.produtoId, quantity.value));

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "danger";
        remove.textContent = "Remover";
        remove.addEventListener("click", () => removeFromCart(item.produtoId));

        controls.append(quantity, remove);
        row.append(text, controls);
        dom.cartItems.append(row);
    });

    dom.cartTotal.textContent = money(total);
    dom.checkoutButton.disabled = !state.carrinho.length;
}

function updateCartQuantity(produtoId, value) {
    const item = state.carrinho.find((candidate) => candidate.produtoId === produtoId);
    if (!item) {
        return;
    }

    const quantity = Math.max(1, Number.parseInt(value, 10) || 1);
    item.quantidade = quantity;
    saveCart();
    renderCart();
}

function removeFromCart(produtoId) {
    state.carrinho = state.carrinho.filter((item) => item.produtoId !== produtoId);
    saveCart();
    renderCart();
}

function renderInvoice(fatura) {
    dom.invoiceBox.replaceChildren();
    if (!fatura) {
        return;
    }

    const title = document.createElement("strong");
    title.textContent = `Fatura #${fatura.encomendaId}`;
    const customer = document.createElement("span");
    customer.textContent = fatura.cliente;
    const total = document.createElement("strong");
    total.textContent = money(fatura.valorTotal || 0);

    const items = document.createElement("div");
    items.className = "list-stack";
    (fatura.itens || []).forEach((item) => {
        const line = document.createElement("span");
        line.textContent = `${item.quantidade} x ${item.produto} · ${money(item.valorTotal || 0)}`;
        items.append(line);
    });

    dom.invoiceBox.append(title, customer, items, total);
}

function renderStatsEmpty() {
    const message = state.user ? "Apenas administradores podem consultar." : "Inicia sessão para consultar.";
    dom.revenueValue.textContent = money(0);
    dom.topProducts.replaceChildren(empty(message));
    dom.lowProducts.replaceChildren(empty(message));
    dom.bestClients.replaceChildren(empty(message));
}

function renderTable(target, headers, rows, readers) {
    target.replaceChildren();
    if (!rows || !rows.length) {
        target.append(empty("Sem dados."));
        return;
    }

    const table = document.createElement("table");
    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");
    headers.forEach((header) => {
        const th = document.createElement("th");
        th.textContent = header;
        headerRow.append(th);
    });
    thead.append(headerRow);

    const tbody = document.createElement("tbody");
    rows.forEach((row) => {
        const tr = document.createElement("tr");
        readers.forEach((reader) => {
            const td = document.createElement("td");
            td.textContent = reader(row);
            tr.append(td);
        });
        tbody.append(tr);
    });

    table.append(thead, tbody);
    target.append(table);
}

function fillSelect(select, options) {
    const currentValue = select.value;
    select.replaceChildren();
    options.forEach((option) => {
        const element = document.createElement("option");
        element.value = option.value;
        element.textContent = option.label;
        select.append(element);
    });
    if ([...select.options].some((option) => option.value === currentValue)) {
        select.value = currentValue;
    }
}

function formToObject(form) {
    return Object.fromEntries(new FormData(form).entries());
}

function optionalNumber(value) {
    return value === "" || value === null || value === undefined ? null : Number(value);
}

function requireSession() {
    if (state.user) {
        return true;
    }
    showNotice("Inicia sessão primeiro.", true);
    showSection("conta");
    return false;
}

function requireAdmin() {
    if (!requireSession()) {
        return false;
    }
    if (isAdmin()) {
        return true;
    }
    showNotice("Apenas administradores podem executar esta ação.", true);
    showSection("catalogo");
    return false;
}

function isAdmin() {
    return state.user?.papel === "ADMIN";
}

function isAdminSection(id) {
    return id === "gestao" || id === "estatisticas";
}

function loadCart() {
    try {
        state.carrinho = JSON.parse(localStorage.getItem("lojaCarrinho")) || [];
    } catch (error) {
        state.carrinho = [];
    }
}

function saveCart() {
    localStorage.setItem("lojaCarrinho", JSON.stringify(state.carrinho));
}

function empty(message) {
    const element = document.createElement("div");
    element.className = "empty";
    element.textContent = message;
    return element;
}

function normalize(value) {
    return String(value || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}

function money(value) {
    return new Intl.NumberFormat("pt-PT", {
        style: "currency",
        currency: "EUR"
    }).format(Number(value || 0));
}

function formatDate(value) {
    if (!value) {
        return "";
    }
    return new Intl.DateTimeFormat("pt-PT", {
        dateStyle: "short",
        timeStyle: "short"
    }).format(new Date(value));
}
