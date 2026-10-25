/**
 * Intelligent Search Engine - Client Application Logic
 * Integrates directly with Java backend via REST endpoints.
 */

const API_BASE = window.location.origin.startsWith('http')
    ? window.location.origin
    : 'http://localhost:8080';

// Application State
const state = {
    category: 'all',
    algo: 'coverage',
    mode: 'single', // 'single' or 'compare'
    compareLimit: 5,
    query: '',
    evalCategory: 'sport',
    evalMode: false,
    suggestions: [],
    highlightedIndex: -1,
    stats: null,
    analytics: null,
    workspace: null,
    selectedCollectionFilter: 'all',
    activeWsTab: 'bookmarks',
    editingNoteDocId: null,
    editingCollectionId: null
};

// DOM Elements
const searchForm = document.getElementById('searchForm');
const queryInput = document.getElementById('queryInput');
const clearBtn = document.getElementById('clearBtn');
const searchBtn = document.getElementById('searchBtn');
const autocompleteDropdown = document.getElementById('autocompleteDropdown');
const autocompleteList = document.getElementById('autocompleteList');
const resultsSection = document.getElementById('resultsSection');
const resultsContainer = document.getElementById('resultsContainer');
const resultsHeader = document.getElementById('resultsHeader');
const resultsSummary = document.getElementById('resultsSummary');
const resultsLatency = document.getElementById('resultsLatency');
const emptyState = document.getElementById('emptyState');
const connectionStatus = document.getElementById('connectionStatus');
const evalToggleBtn = document.getElementById('evalToggleBtn');
const evalPanel = document.getElementById('evalPanel');
const evalCategorySelect = document.getElementById('evalCategorySelect');

// Comparison Lab Elements
const compareAllBtn = document.getElementById('compareAllBtn');
const comparisonSection = document.getElementById('comparisonSection');
const comparisonQueryTitle = document.getElementById('comparisonQueryTitle');
const comparisonMetricsBar = document.getElementById('comparisonMetricsBar');
const compareLimitSelect = document.getElementById('compareLimitSelect');
const btnReturnToSingle = document.getElementById('btnReturnToSingle');
const btnSwitchToCompare = document.getElementById('btnSwitchToCompare');

// Modals
const docModal = document.getElementById('docModal');
const docModalClose = document.getElementById('docModalClose');
const docModalTitle = document.getElementById('docModalTitle');
const docModalMeta = document.getElementById('docModalMeta');
const docModalBody = document.getElementById('docModalBody');

const archModal = document.getElementById('archModal');
const archModalBtn = document.getElementById('archModalBtn');
const archModalClose = document.getElementById('archModalClose');

let suggestDebounce = null;

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

async function initApp() {
    setupEventListeners();
    await loadBackendStats();
    await loadAnalytics();
    await loadWorkspace();
    initGoogleComparisonLab();
    await loadGoogleComparisonHistory();

    // Check URL params for algorithm, mode, and query
    const urlParams = new URLSearchParams(window.location.search);
    const initialMode = urlParams.get('mode');
    const initialAlgo = urlParams.get('algo');

    if (initialMode === 'compare') {
        state.mode = 'compare';
        document.querySelectorAll('.algo-pill').forEach(p => {
            p.classList.remove('active');
            const dot = p.querySelector('.algo-dot');
            if (dot) dot.classList.remove('active-dot');
        });
        if (compareAllBtn) {
            compareAllBtn.classList.add('active');
            const dot = compareAllBtn.querySelector('.algo-dot');
            if (dot) dot.classList.add('active-dot');
        }
    } else if (initialAlgo && (initialAlgo === 'bm25' || initialAlgo === 'baseline' || initialAlgo === 'coverage' || initialAlgo === 'semantic')) {
        state.algo = initialAlgo;
        document.querySelectorAll('.algo-pill').forEach(p => {
            if (p.dataset.algo === initialAlgo) {
                p.classList.add('active');
                const dot = p.querySelector('.algo-dot');
                if (dot) dot.classList.add('active-dot');
            } else {
                p.classList.remove('active');
                const dot = p.querySelector('.algo-dot');
                if (dot) dot.classList.remove('active-dot');
            }
        });
    }

    const initialQuery = urlParams.get('q');
    if (initialQuery) {
        queryInput.value = initialQuery;
        if (state.mode === 'compare') {
            performComparisonSearch(initialQuery);
        } else {
            performSearch(initialQuery);
        }
    }
}

// =============================================================================
// BACKEND STATISTICS
// =============================================================================

async function loadBackendStats() {
    try {
        const response = await fetch(`${API_BASE}/api/stats`);
        if (!response.ok) throw new Error('Backend responded with error');
        const data = await response.json();
        state.stats = data;

        // Update Stats UI
        document.getElementById('statTotalDocs').textContent = Number(data.totalDocuments).toLocaleString();
        document.getElementById('statTotalTerms').textContent = Number(data.totalTerms).toLocaleString();
        document.getElementById('statTotalCategories').textContent = data.categories.length;

        // Update category pills with real counts
        updateCategoryPillCounts(data.categoryCounts, data.totalDocuments);

        // Update status badge
        connectionStatus.className = 'status-badge';
        connectionStatus.innerHTML = '<span class="status-dot"></span> Backend Connected (Port 8080)';
    } catch (error) {
        console.warn('Could not connect to Java backend:', error);
        connectionStatus.className = 'status-badge disconnected';
        connectionStatus.innerHTML = '<span class="status-dot"></span> Backend Offline (Check Java Server)';
    }
}

function updateCategoryPillCounts(counts, total) {
    const allPill = document.querySelector('.category-pill[data-category="all"] .pill-count');
    if (allPill) allPill.textContent = total;

    if (!counts) return;
    for (const [cat, count] of Object.entries(counts)) {
        const pill = document.querySelector(`.category-pill[data-category="${cat.toLowerCase()}"] .pill-count`);
        if (pill) pill.textContent = count;
    }
}

// =============================================================================
// EVENT LISTENERS
// =============================================================================

function setupEventListeners() {
    // Form submission
    searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        hideAutocomplete();
        const q = queryInput.value.trim();
        if (q) performSearch(q);
    });

    // Clear input button
    clearBtn.addEventListener('click', () => {
        queryInput.value = '';
        clearBtn.style.display = 'none';
        hideAutocomplete();
        queryInput.focus();
    });

    // Input changes for Trie prefix suggestions
    queryInput.addEventListener('input', handleQueryInput);

    // Keyboard navigation in suggestions
    queryInput.addEventListener('keydown', handleKeyNavigation);

    // Algorithm comparison lab pills click
    document.querySelectorAll('.algo-pill').forEach((pill) => {
        pill.addEventListener('click', () => {
            const algo = pill.dataset.algo;
            if (algo === 'compare') {
                switchToCompareMode();
            } else {
                switchToSingleMode(algo);
            }
        });
    });

    if (btnSwitchToCompare) {
        btnSwitchToCompare.addEventListener('click', () => {
            switchToCompareMode();
        });
    }

    if (btnReturnToSingle) {
        btnReturnToSingle.addEventListener('click', () => {
            switchToSingleMode(state.algo || 'coverage');
        });
    }

    if (compareLimitSelect) {
        compareLimitSelect.addEventListener('change', (e) => {
            state.compareLimit = parseInt(e.target.value, 10) || 5;
            const q = queryInput.value.trim() || state.query;
            if (state.mode === 'compare' && q) {
                performComparisonSearch(q);
            }
        });
    }

    // Category pills click
    document.querySelectorAll('.category-pill').forEach((pill) => {
        pill.addEventListener('click', () => {
            document.querySelectorAll('.category-pill').forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            state.category = pill.dataset.category;
            const q = queryInput.value.trim();
            if (q) performSearch(q);
        });
    });

    // Quick query chips click
    document.querySelectorAll('.chip-btn').forEach((chip) => {
        chip.addEventListener('click', () => {
            const query = chip.dataset.query;
            queryInput.value = query;
            clearBtn.style.display = 'flex';
            performSearch(query);
        });
    });

    // Evaluation mode toggle
    evalToggleBtn.addEventListener('click', () => {
        state.evalMode = !state.evalMode;
        if (state.evalMode) {
            evalPanel.classList.add('show');
            evalToggleBtn.style.background = 'rgba(139, 92, 246, 0.25)';
            evalToggleBtn.style.borderColor = '#8b5cf6';
            if (state.query) runEvaluation(state.query, state.evalCategory);
        } else {
            evalPanel.classList.remove('show');
            evalToggleBtn.style.background = '';
            evalToggleBtn.style.borderColor = '';
        }
    });

    evalCategorySelect.addEventListener('change', (e) => {
        state.evalCategory = e.target.value;
        if (state.query) runEvaluation(state.query, state.evalCategory);
    });

    // Document Modal Close
    docModalClose.addEventListener('click', () => docModal.classList.remove('active'));
    docModal.addEventListener('click', (e) => {
        if (e.target === docModal) docModal.classList.remove('active');
    });

    // Architecture Modal Close
    archModalBtn.addEventListener('click', () => archModal.classList.add('active'));
    archModalClose.addEventListener('click', () => archModal.classList.remove('active'));
    archModal.addEventListener('click', (e) => {
        if (e.target === archModal) archModal.classList.remove('active');
    });

    // Close autocomplete on click outside
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.search-box-container')) {
            hideAutocomplete();
        }
    });

    // Feature 2: Analytics controls
    const analyticsNavBtn = document.getElementById('analyticsNavBtn');
    if (analyticsNavBtn) {
        analyticsNavBtn.addEventListener('click', () => {
            const el = document.getElementById('analyticsDashboard');
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    }

    const refreshAnalyticsBtn = document.getElementById('refreshAnalyticsBtn');
    if (refreshAnalyticsBtn) {
        refreshAnalyticsBtn.addEventListener('click', () => {
            const icon = document.getElementById('refreshIcon');
            if (icon) {
                icon.classList.remove('spin-once');
                void icon.offsetWidth;
                icon.classList.add('spin-once');
            }
            loadAnalytics();
        });
    }

    const clearAnalyticsBtn = document.getElementById('clearAnalyticsBtn');
    if (clearAnalyticsBtn) {
        clearAnalyticsBtn.addEventListener('click', handleClearAnalytics);
    }

    const emptyStateSampleBtn = document.getElementById('emptyStateSampleBtn');
    if (emptyStateSampleBtn) {
        emptyStateSampleBtn.addEventListener('click', () => {
            queryInput.value = 'quantum cosmology';
            clearBtn.style.display = 'flex';
            performSearch('quantum cosmology');
        });
    }

    // Feature 3: Research Workspace Navigation & Actions
    const workspaceNavBtn = document.getElementById('workspaceNavBtn');
    if (workspaceNavBtn) {
        workspaceNavBtn.addEventListener('click', () => {
            const el = document.getElementById('researchWorkspace');
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    }

    const wsTabBookmarks = document.getElementById('wsTabBookmarks');
    const wsTabSearches = document.getElementById('wsTabSearches');
    if (wsTabBookmarks && wsTabSearches) {
        wsTabBookmarks.addEventListener('click', () => switchWorkspaceTab('bookmarks'));
        wsTabSearches.addEventListener('click', () => switchWorkspaceTab('searches'));
    }

    const btnOpenCreateCollection = document.getElementById('btnOpenCreateCollection');
    if (btnOpenCreateCollection) {
        btnOpenCreateCollection.addEventListener('click', () => {
            state.editingCollectionId = null;
            const formCard = document.getElementById('collectionFormCard');
            const titleEl = document.getElementById('colFormTitle');
            const inputEl = document.getElementById('collectionNameInput');
            if (titleEl) titleEl.textContent = 'Create New Collection';
            if (inputEl) inputEl.value = '';
            if (formCard) formCard.style.display = 'block';
            if (inputEl) inputEl.focus();
        });
    }

    const btnCancelCollection = document.getElementById('btnCancelCollection');
    if (btnCancelCollection) {
        btnCancelCollection.addEventListener('click', () => {
            const formCard = document.getElementById('collectionFormCard');
            if (formCard) formCard.style.display = 'none';
            state.editingCollectionId = null;
        });
    }

    const btnSaveCollection = document.getElementById('btnSaveCollection');
    if (btnSaveCollection) {
        btnSaveCollection.addEventListener('click', handleSaveCollectionSubmit);
    }

    const collectionNameInput = document.getElementById('collectionNameInput');
    if (collectionNameInput) {
        collectionNameInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleSaveCollectionSubmit();
            } else if (e.key === 'Escape') {
                const formCard = document.getElementById('collectionFormCard');
                if (formCard) formCard.style.display = 'none';
                state.editingCollectionId = null;
            }
        });
    }

    const btnClearWorkspace = document.getElementById('btnClearWorkspace');
    if (btnClearWorkspace) {
        btnClearWorkspace.addEventListener('click', handleClearWorkspace);
    }

    const btnSaveCurrentQuery = document.getElementById('btnSaveCurrentQuery');
    if (btnSaveCurrentQuery) {
        btnSaveCurrentQuery.addEventListener('click', () => handleSaveCurrentQuery(btnSaveCurrentQuery));
    }

    const btnSaveCompareQuery = document.getElementById('btnSaveCompareQuery');
    if (btnSaveCompareQuery) {
        btnSaveCompareQuery.addEventListener('click', () => handleSaveCurrentQuery(btnSaveCompareQuery));
    }
}

// =============================================================================
// COMPRESSED TRIE AUTOCOMPLETE
// =============================================================================

function handleQueryInput() {
    const val = queryInput.value;
    clearBtn.style.display = val.length > 0 ? 'flex' : 'none';

    clearTimeout(suggestDebounce);
    const trimmed = val.trim();

    if (!trimmed) {
        hideAutocomplete();
        return;
    }

    // Debounce to avoid overloading on fast typing
    suggestDebounce = setTimeout(async () => {
        // Extract the last word being typed for multi-word queries
        const tokens = trimmed.split(/\s+/);
        const lastToken = tokens[tokens.length - 1];

        if (lastToken.length < 2) {
            hideAutocomplete();
            return;
        }

        try {
            const res = await fetch(`${API_BASE}/api/suggest?q=${encodeURIComponent(lastToken)}`);
            if (!res.ok) return;
            const data = await res.json();

            if (data.suggestions && data.suggestions.length > 0) {
                state.suggestions = data.suggestions;
                renderAutocomplete(tokens.slice(0, -1), lastToken, data.suggestions);
            } else {
                hideAutocomplete();
            }
        } catch (e) {
            console.warn('Prefix suggestion fetch failed:', e);
            hideAutocomplete();
        }
    }, 120);
}

function renderAutocomplete(prefixTokens, currentPrefix, suggestions) {
    autocompleteList.innerHTML = '';
    state.highlightedIndex = -1;

    const baseQuery = prefixTokens.length > 0 ? prefixTokens.join(' ') + ' ' : '';

    suggestions.forEach((word, index) => {
        const fullSuggestion = baseQuery + word;
        const li = document.createElement('li');
        li.className = 'autocomplete-item';
        li.setAttribute('data-index', index);
        li.setAttribute('data-full-query', fullSuggestion);

        // Highlight the matched prefix in the word
        const matchPart = word.substring(0, currentPrefix.length);
        const restPart = word.substring(currentPrefix.length);

        li.innerHTML = `
            <span>
                ${baseQuery}<span class="match-text">${escapeHtml(matchPart)}</span>${escapeHtml(restPart)}
            </span>
            <span class="item-type">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="7" y1="17" x2="17" y2="7"></line>
                    <polyline points="7 7 17 7 17 17"></polyline>
                </svg>
                Trie Term
            </span>
        `;

        li.addEventListener('click', () => {
            queryInput.value = fullSuggestion;
            hideAutocomplete();
            performSearch(fullSuggestion);
        });

        autocompleteList.appendChild(li);
    });

    autocompleteDropdown.classList.add('show');
}

function hideAutocomplete() {
    autocompleteDropdown.classList.remove('show');
    state.suggestions = [];
    state.highlightedIndex = -1;
}

function handleKeyNavigation(e) {
    if (!autocompleteDropdown.classList.contains('show')) return;

    const items = autocompleteList.querySelectorAll('.autocomplete-item');
    if (!items.length) return;

    if (e.key === 'ArrowDown') {
        e.preventDefault();
        state.highlightedIndex = (state.highlightedIndex + 1) % items.length;
        updateHighlight(items);
    } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        state.highlightedIndex = (state.highlightedIndex - 1 + items.length) % items.length;
        updateHighlight(items);
    } else if (e.key === 'Enter') {
        if (state.highlightedIndex >= 0 && items[state.highlightedIndex]) {
            e.preventDefault();
            const chosen = items[state.highlightedIndex].getAttribute('data-full-query');
            queryInput.value = chosen;
            hideAutocomplete();
            performSearch(chosen);
        }
    } else if (e.key === 'Escape') {
        hideAutocomplete();
    }
}

function updateHighlight(items) {
    items.forEach((item, idx) => {
        if (idx === state.highlightedIndex) {
            item.classList.add('selected');
            item.scrollIntoView({ block: 'nearest' });
        } else {
            item.classList.remove('selected');
        }
    });
}

// =============================================================================
// SEARCH EXECUTION & RESULTS
// =============================================================================

function switchToCompareMode() {
    state.mode = 'compare';
    document.querySelectorAll('.algo-pill').forEach(p => {
        p.classList.remove('active');
        const dot = p.querySelector('.algo-dot');
        if (dot) dot.classList.remove('active-dot');
    });
    if (compareAllBtn) {
        compareAllBtn.classList.add('active');
        const dot = compareAllBtn.querySelector('.algo-dot');
        if (dot) dot.classList.add('active-dot');
    }

    resultsSection.style.display = 'none';
    const q = queryInput.value.trim() || state.query;
    if (q) {
        performComparisonSearch(q);
    } else {
        comparisonSection.style.display = 'block';
    }
}

function switchToSingleMode(algo) {
    state.mode = 'single';
    state.algo = algo || 'coverage';
    document.querySelectorAll('.algo-pill').forEach(p => {
        p.classList.remove('active');
        const dot = p.querySelector('.algo-dot');
        if (dot) dot.classList.remove('active-dot');
    });
    const activePill = document.querySelector(`.algo-pill[data-algo="${state.algo}"]`);
    if (activePill) {
        activePill.classList.add('active');
        const dot = activePill.querySelector('.algo-dot');
        if (dot) dot.classList.add('active-dot');
    }

    comparisonSection.style.display = 'none';
    const q = queryInput.value.trim() || state.query;
    if (q) {
        performSearch(q);
    }
}

async function performSearch(query) {
    state.query = query;

    if (state.mode === 'compare') {
        return performComparisonSearch(query);
    }

    comparisonSection.style.display = 'none';
    searchBtn.disabled = true;
    searchBtn.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
            <line x1="12" y1="2" x2="12" y2="6"></line>
            <line x1="12" y1="18" x2="12" y2="22"></line>
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
            <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
            <line x1="2" y1="12" x2="6" y2="12"></line>
            <line x1="18" y1="12" x2="22" y2="12"></line>
            <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
            <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
        </svg> Searching...`;

    try {
        const start = performance.now();
        const url = `${API_BASE}/api/search?q=${encodeURIComponent(query)}&category=${encodeURIComponent(state.category)}&algo=${encodeURIComponent(state.algo)}&limit=25`;
        const res = await fetch(url);
        const clientLatency = Math.round(performance.now() - start);
        if (!res.ok) throw new Error('Search failed');
        const data = await res.json();

        renderSearchResults(data);

        // Report genuine client RTT if searchId returned, and refresh analytics
        if (data.searchId) {
            reportRoundTripTime(data.searchId, clientLatency);
        } else {
            loadAnalytics();
        }

        if (state.evalMode) {
            runEvaluation(query, state.evalCategory);
        }
    } catch (err) {
        console.error('Search request failed:', err);
        showErrorState('Search request could not be completed. Please ensure the Java backend is running.');
    } finally {
        searchBtn.disabled = false;
        searchBtn.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg> Search`;
    }
}

// =============================================================================
// 3-WAY RANKING COMPARISON LAB EXECUTION
// =============================================================================

async function performComparisonSearch(query) {
    state.query = query;
    resultsSection.style.display = 'none';
    comparisonSection.style.display = 'block';

    if (comparisonQueryTitle) {
        comparisonQueryTitle.innerHTML = `3-Way Algorithm Comparison for "<em>${escapeHtml(query)}</em>"`;
    }

    searchBtn.disabled = true;
    searchBtn.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
            <line x1="12" y1="2" x2="12" y2="6"></line>
            <line x1="12" y1="18" x2="12" y2="22"></line>
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
            <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
            <line x1="2" y1="12" x2="6" y2="12"></line>
            <line x1="18" y1="12" x2="22" y2="12"></line>
            <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
            <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
        </svg> Comparing...`;

    // Render skeleton loading states in each column
    renderComparisonSkeletons('resultsCoverage');
    renderComparisonSkeletons('resultsBm25');
    renderComparisonSkeletons('resultsBaseline');
    if (comparisonMetricsBar) comparisonMetricsBar.innerHTML = '';

    const limit = state.compareLimit || 5;
    const cat = state.category || 'all';

    const compareId = 'cmp_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const compStart = performance.now();

    const algos = [
        { id: 'coverage', label: 'Coverage-Aware TF-IDF', metaEl: 'metaCoverage', resEl: 'resultsCoverage' },
        { id: 'bm25', label: 'Okapi BM25', metaEl: 'metaBm25', resEl: 'resultsBm25' },
        { id: 'baseline', label: 'Baseline TF-IDF', metaEl: 'metaBaseline', resEl: 'resultsBaseline' }
    ];

    const resultsMap = {};

    try {
        const promises = algos.map(async (algoObj) => {
            const start = performance.now();
            try {
                const url = `${API_BASE}/api/search?q=${encodeURIComponent(query)}&category=${encodeURIComponent(cat)}&algo=${algoObj.id}&limit=${limit}&mode=compare&compareId=${encodeURIComponent(compareId)}`;
                const res = await fetch(url);
                const clientLatency = Math.round(performance.now() - start);

                if (!res.ok) {
                    throw new Error(`Server returned HTTP ${res.status}`);
                }
                const data = await res.json();
                resultsMap[algoObj.id] = { data, clientLatency };
                renderComparisonColumn(algoObj, data, clientLatency, query);
            } catch (err) {
                console.error(`Comparison error for ${algoObj.id}:`, err);
                renderComparisonColumnError(algoObj, err.message);
            }
        });

        await Promise.allSettled(promises);

        const compTotalRtt = Math.round(performance.now() - compStart);
        reportRoundTripTime(compareId, compTotalRtt);

        // Render cross-model consensus analysis
        renderConsensusMetrics(query, resultsMap);

        if (state.evalMode) {
            runEvaluation(query, state.evalCategory);
        }
    } finally {
        searchBtn.disabled = false;
        searchBtn.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg> Search`;
        comparisonSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

function renderComparisonSkeletons(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.innerHTML = `
        <div class="comp-skeleton-card">
            <div class="skeleton-shimmer skeleton-top"></div>
            <div class="skeleton-shimmer skeleton-meta"></div>
            <div class="skeleton-shimmer skeleton-body"></div>
        </div>
        <div class="comp-skeleton-card">
            <div class="skeleton-shimmer skeleton-top"></div>
            <div class="skeleton-shimmer skeleton-meta"></div>
            <div class="skeleton-shimmer skeleton-body"></div>
        </div>
        <div class="comp-skeleton-card">
            <div class="skeleton-shimmer skeleton-top"></div>
            <div class="skeleton-shimmer skeleton-meta"></div>
            <div class="skeleton-shimmer skeleton-body"></div>
        </div>
    `;
}

function renderComparisonColumn(algoObj, data, clientLatency, query) {
    const metaEl = document.getElementById(algoObj.metaEl);
    const resEl = document.getElementById(algoObj.resEl);
    if (!resEl) return;

    if (metaEl) {
        metaEl.innerHTML = `
            <span class="col-matches"><strong>${data.totalMatches.toLocaleString()}</strong> matches</span>
            <span class="col-latency" title="Server Execution: ${data.executionTimeMs}ms | Client Round-Trip: ${clientLatency}ms">
                ${data.executionTimeMs} ms
            </span>
        `;
    }

    resEl.innerHTML = '';

    if (!data.results || data.results.length === 0) {
        resEl.innerHTML = `
            <div class="col-empty">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="opacity: 0.5;">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <span>No matching documents</span>
            </div>
        `;
        return;
    }

    data.results.forEach((item) => {
        const card = document.createElement('div');
        card.className = 'comp-card';

        const categoryClass = (item.category || '').toLowerCase();
        const highlightedSnippet = highlightQueryWords(item.snippet, query);

        const matchedTerms = item.matchedTerms || [];
        const matchedCount = item.matchedTermsCount != null ? item.matchedTermsCount : matchedTerms.length;
        const totalValid = item.totalValidTerms != null ? item.totalValidTerms : (data.totalValidTerms || 1);
        const coveragePct = parseFloat(item.coveragePercentage != null
            ? item.coveragePercentage
            : (totalValid > 0 ? (matchedCount / totalValid) * 100 : 0));

        const covClass = coveragePct >= 100.0 ? 'cov-full' : 'cov-partial';

        let scoreLabel = 'Score';
        if (algoObj.id === 'bm25') scoreLabel = 'BM25';
        else if (algoObj.id === 'baseline') scoreLabel = 'TF-IDF';
        else scoreLabel = 'Cov-TFIDF';

        let termsChips = '';
        if (matchedTerms.length > 0) {
            termsChips = `
                <div class="comp-matched-terms">
                    ${matchedTerms.map(t => `<span class="comp-term-chip">${escapeHtml(t)}</span>`).join('')}
                </div>
            `;
        }

        const isSaved = isDocumentBookmarked(item.id);

        card.innerHTML = `
            <div class="comp-card-top">
                <div class="comp-id-row">
                    <span class="comp-rank-badge">#${item.rank}</span>
                    <span class="comp-doc-id">Doc #${escapeHtml(item.id)}.txt</span>
                    <span class="category-tag ${categoryClass}">${escapeHtml(item.category)}</span>
                </div>
                <div class="comp-score-badge" title="${algoObj.label} Score: ${item.score}">
                    ${scoreLabel}: ${item.score}
                </div>
            </div>

            <div class="comp-card-metrics">
                <span class="comp-cov-pill ${covClass}" title="Matched ${matchedCount} of ${totalValid} valid query terms">
                    ${matchedCount}/${totalValid} terms (${coveragePct.toFixed(0)}%)
                </span>
                ${termsChips}
            </div>

            <div class="comp-snippet" title="${escapeHtml(item.snippet)}">
                ${highlightedSnippet}
            </div>

            <div class="comp-card-footer">
                <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                    <button type="button" class="btn-comp-read" data-doc-id="${escapeHtml(item.id)}" data-doc-cat="${escapeHtml(item.category)}">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                            <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                        </svg>
                        Read
                    </button>
                    <button type="button" class="btn-bookmark-action ${isSaved ? 'saved' : ''}" data-doc-id="${escapeHtml(item.id)}" data-doc-cat="${escapeHtml(item.category)}">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="${isSaved ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
                            <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                        </svg>
                        <span class="bm-label">${isSaved ? 'Bookmarked' : 'Bookmark'}</span>
                    </button>
                </div>
            </div>
        `;

        card.querySelector('.btn-comp-read').addEventListener('click', (e) => {
            e.stopPropagation();
            openDocumentModal(item.id, item.category);
        });

        card.querySelector('.btn-bookmark-action').addEventListener('click', (e) => {
            e.stopPropagation();
            toggleBookmark(item.id, item.category);
        });

        resEl.appendChild(card);
    });
}

function renderComparisonColumnError(algoObj, msg) {
    const resEl = document.getElementById(algoObj.resEl);
    const metaEl = document.getElementById(algoObj.metaEl);
    if (metaEl) {
        metaEl.innerHTML = `<span class="col-matches" style="color: #f87171;">Failed</span>`;
    }
    if (resEl) {
        resEl.innerHTML = `
            <div class="col-error">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                <span>${escapeHtml(msg || 'Request failed')}</span>
            </div>
        `;
    }
}

function renderConsensusMetrics(query, resultsMap) {
    if (!comparisonMetricsBar) return;

    const covDocs = (resultsMap.coverage && resultsMap.coverage.data && resultsMap.coverage.data.results) || [];
    const bm25Docs = (resultsMap.bm25 && resultsMap.bm25.data && resultsMap.bm25.data.results) || [];
    const baseDocs = (resultsMap.baseline && resultsMap.baseline.data && resultsMap.baseline.data.results) || [];

    if (covDocs.length === 0 && bm25Docs.length === 0 && baseDocs.length === 0) {
        comparisonMetricsBar.innerHTML = '';
        return;
    }

    // Rank 1 analysis
    const covTop = covDocs[0] ? covDocs[0].id : null;
    const bmTop = bm25Docs[0] ? bm25Docs[0].id : null;
    const baseTop = baseDocs[0] ? baseDocs[0].id : null;

    let rank1Text = 'No documents returned';
    if (covTop && bmTop && baseTop) {
        if (covTop === bmTop && covTop === baseTop) {
            rank1Text = `Full Consensus: Doc #${covTop} ranked #1 by all 3 algorithms`;
        } else if (covTop === bmTop) {
            rank1Text = `Coverage & BM25 agree on Doc #${covTop}; Baseline selected Doc #${baseTop}`;
        } else if (covTop === baseTop) {
            rank1Text = `Coverage & Baseline agree on Doc #${covTop}; BM25 selected Doc #${bmTop}`;
        } else {
            rank1Text = `Divergent: Cov (#${covTop}), BM25 (#${bmTop}), Base (#${baseTop})`;
        }
    } else if (covTop) {
        rank1Text = `Rank #1: Doc #${covTop}`;
    }

    // Top-K Overlap count
    const setCov = new Set(covDocs.map(d => d.id));
    const setBm = new Set(bm25Docs.map(d => d.id));
    const setBase = new Set(baseDocs.map(d => d.id));

    let sharedCount = 0;
    setCov.forEach(id => {
        if (setBm.has(id) && setBase.has(id)) {
            sharedCount++;
        }
    });

    const evaluatedK = Math.max(covDocs.length, bm25Docs.length, baseDocs.length);

    // Latencies
    const covMs = resultsMap.coverage?.data?.executionTimeMs ?? '--';
    const bmMs = resultsMap.bm25?.data?.executionTimeMs ?? '--';
    const baseMs = resultsMap.baseline?.data?.executionTimeMs ?? '--';

    comparisonMetricsBar.innerHTML = `
        <div class="consensus-card">
            <div class="consensus-icon rank1">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="8" r="7"></circle>
                    <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"></polyline>
                </svg>
            </div>
            <div>
                <div class="consensus-label">Rank #1 Agreement</div>
                <div class="consensus-value">${escapeHtml(rank1Text)}</div>
            </div>
        </div>

        <div class="consensus-card">
            <div class="consensus-icon shared">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
            </div>
            <div>
                <div class="consensus-label">Top-${evaluatedK} Overlap</div>
                <div class="consensus-value"><strong>${sharedCount}</strong> of ${evaluatedK} documents common to all 3 algorithms</div>
            </div>
        </div>

        <div class="consensus-card">
            <div class="consensus-icon speed">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
            </div>
            <div>
                <div class="consensus-label">Server Latency Profile</div>
                <div class="consensus-value">Cov: ${covMs}ms · BM25: ${bmMs}ms · Base: ${baseMs}ms</div>
            </div>
        </div>
    `;
}

function renderSearchResults(data) {
    resultsSection.style.display = 'block';
    resultsContainer.innerHTML = '';

    if (!data.results || data.results.length === 0) {
        resultsHeader.style.display = 'none';
        emptyState.style.display = 'block';
        document.getElementById('emptyQueryText').textContent = data.query;
        return;
    }

    emptyState.style.display = 'none';
    resultsHeader.style.display = 'flex';

    const algoNames = {
        'coverage': 'Coverage-Aware TF-IDF',
        'semantic': 'Semantic Search (LSA)',
        'bm25': 'Okapi BM25',
        'baseline': 'Baseline TF-IDF'
    };
    const algoLabel = algoNames[data.algo] || 'Coverage-Aware TF-IDF';

    resultsSummary.innerHTML = `Found <strong>${data.totalMatches}</strong> matching document${data.totalMatches === 1 ? '' : 's'} for "<em>${escapeHtml(data.query)}</em>"`;
    resultsLatency.textContent = `${data.executionTimeMs} ms · ${algoLabel}`;

    data.results.forEach((item) => {
        const card = document.createElement('div');
        card.className = 'result-card';

        const categoryClass = (item.category || '').toLowerCase();
        const highlightedSnippet = highlightQueryWords(item.snippet, data.query, item.relatedKeywords);

        const matchedTerms = item.matchedTerms || [];
        const matchedCount = item.matchedTermsCount != null ? item.matchedTermsCount : matchedTerms.length;
        const totalValid = item.totalValidTerms != null ? item.totalValidTerms : (data.totalValidTerms || 1);
        const coveragePct = item.coveragePercentage != null
            ? item.coveragePercentage
            : ((totalValid > 0 ? (matchedCount / totalValid) * 100 : 0).toFixed(1));

        let scoreLabel = 'Score';
        let metricsHtml = '';
        let explanationBarHtml = '';

        if (data.algo === 'semantic') {
            scoreLabel = 'Semantic Sim';
            metricsHtml = `
                <div class="coverage-badge" style="border-color: rgba(168, 85, 247, 0.4); color: #d8b4fe;" title="128-D Latent Semantic Concept Space Alignment">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <circle cx="12" cy="12" r="10"></circle>
                        <path d="M12 6v6l4 2"></path>
                    </svg>
                    128-D Concept Space
                </div>
                <div class="score-badge semantic-score" title="Cosine similarity score in 128-D latent semantic concept space">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                    </svg>
                    Semantic Sim: ${item.score}
                </div>
            `;

            const conceptsHtml = (item.topConcepts && item.topConcepts.length > 0)
                ? `<div class="semantic-chips-row"><span style="color: #c084fc; font-weight: 600;">Active Latent Topics:</span> ${item.topConcepts.map(c => `<span class="semantic-concept-chip">💡 ${escapeHtml(c)}</span>`).join('')}</div>`
                : '';
            const relatedHtml = (item.relatedKeywords && item.relatedKeywords.length > 0)
                ? `<div class="semantic-chips-row" style="margin-top: 4px;"><span style="color: #38bdf8; font-weight: 600;">Related Document Terms:</span> ${item.relatedKeywords.map(k => `<span class="semantic-keyword-chip">🔗 ${escapeHtml(k)}</span>`).join('')}</div>`
                : '';

            explanationBarHtml = `
                <div class="semantic-explain-box">
                    <div class="semantic-explain-header">
                        <span>🧠 Semantic Explainability:</span>
                        <span>${escapeHtml(item.explanation || 'Conceptually aligned in 128-D latent semantic concept space')}</span>
                    </div>
                    ${conceptsHtml}
                    ${relatedHtml}
                </div>
            `;
        } else {
            if (data.algo === 'bm25') {
                scoreLabel = 'BM25';
            } else if (data.algo === 'baseline') {
                scoreLabel = 'Baseline';
            } else {
                scoreLabel = 'Coverage TF-IDF';
            }

            metricsHtml = `
                <div class="coverage-badge" title="Query Term Coverage: ${matchedCount} of ${totalValid} valid query terms found in this document">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                    Term Coverage: ${matchedCount}/${totalValid} (${coveragePct}%)
                </div>
                <div class="score-badge" title="${algoLabel} Ranking Score">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                    </svg>
                    ${scoreLabel}: ${item.score}
                </div>
            `;

            if (matchedTerms.length > 0) {
                const chipsHtml = matchedTerms
                    .map(t => `<span class="matched-chip">${escapeHtml(t)}</span>`)
                    .join('');
                explanationBarHtml = `
                    <div class="explain-bar">
                        <span class="explain-bar-title">Matched Terms:</span>
                        <div class="matched-chips-list">${chipsHtml}</div>
                        <span class="explain-sep">·</span>
                        <span class="explain-metric">Term Coverage: <strong>${coveragePct}%</strong> (${matchedCount}/${totalValid})</span>
                    </div>
                `;
            }
        }

        const isSaved = isDocumentBookmarked(item.id);

        card.innerHTML = `
            <div class="result-top-bar">
                <div class="result-identity">
                    <span class="rank-badge">#${item.rank}</span>
                    <span class="doc-id">Doc #${escapeHtml(item.id)}.txt</span>
                    <span class="category-tag ${categoryClass}">${escapeHtml(item.category)}</span>
                </div>
                <div class="result-metrics">
                    ${metricsHtml}
                </div>
            </div>
            ${explanationBarHtml}
            <div class="result-snippet">${highlightedSnippet}</div>
            <div class="result-actions">
                <button class="btn-read-doc" data-doc-id="${escapeHtml(item.id)}">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                        <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                    </svg>
                    Read Document
                </button>
                <button type="button" class="btn-bookmark-action ${isSaved ? 'saved' : ''}" data-doc-id="${escapeHtml(item.id)}" data-doc-cat="${escapeHtml(item.category)}">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="${isSaved ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
                        <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                    </svg>
                    <span class="bm-label">${isSaved ? 'Bookmarked' : 'Bookmark'}</span>
                </button>
            </div>
        `;

        // Read Document button click
        card.querySelector('.btn-read-doc').addEventListener('click', () => {
            openDocumentModal(item.id, item.category);
        });

        // Bookmark button click
        card.querySelector('.btn-bookmark-action').addEventListener('click', () => {
            toggleBookmark(item.id, item.category);
        });

        resultsContainer.appendChild(card);
    });

    resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function highlightQueryWords(snippet, query, relatedKeywords) {
    if (!snippet) return '';
    let escaped = escapeHtml(snippet);
    const terms = query
        .toLowerCase()
        .replace(/[^a-z0-9\s]/g, ' ')
        .split(/\s+/)
        .filter(t => t.length > 0);

    // Highlight query terms first
    terms.sort((a, b) => b.length - a.length);
    terms.forEach(term => {
        const regex = new RegExp(`(\\b${escapeRegex(term)}[a-z0-9]*)`, 'gi');
        escaped = escaped.replace(regex, '<mark>$1</mark>');
    });

    // Highlight related semantic words with distinct styling
    if (relatedKeywords && Array.isArray(relatedKeywords)) {
        const rWords = relatedKeywords.filter(w => !terms.includes(w.toLowerCase()));
        rWords.sort((a, b) => b.length - a.length);
        rWords.forEach(w => {
            const regex = new RegExp(`(\\b${escapeRegex(w)}[a-z0-9]*)`, 'gi');
            escaped = escaped.replace(regex, '<mark class="semantic-term-mark">$1</mark>');
        });
    }

    return escaped;
}

// =============================================================================
// SEARCH EVALUATION
// =============================================================================

async function runEvaluation(query, category) {
    try {
        const url = `${API_BASE}/api/evaluate?q=${encodeURIComponent(query)}&category=${encodeURIComponent(category)}&algo=${encodeURIComponent(state.algo)}`;
        const res = await fetch(url);
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById('metricPrecision').textContent = (parseFloat(data.precision) * 100).toFixed(1) + '%';
        document.getElementById('metricRecall').textContent = (parseFloat(data.recall) * 100).toFixed(2) + '%';
        document.getElementById('metricF1').textContent = data.f1Score;
    } catch (e) {
        console.warn('Evaluation calculation failed:', e);
    }
}

// =============================================================================
// FULL DOCUMENT READER MODAL
// =============================================================================

async function openDocumentModal(docId, category) {
    docModalTitle.textContent = `Document #${docId}.txt`;
    docModalMeta.innerHTML = `<span class="category-tag ${(category || '').toLowerCase()}">${escapeHtml(category)}</span> Loading document from dataset...`;
    docModalBody.textContent = 'Fetching document content...';
    docModal.classList.add('active');

    try {
        const res = await fetch(`${API_BASE}/api/document?id=${encodeURIComponent(docId)}&category=${encodeURIComponent(category || '')}`);
        if (!res.ok) throw new Error('Document not found');
        const doc = await res.json();

        const wordsCount = doc.content.split(/\s+/).filter(w => w.length > 0).length;

        docModalMeta.innerHTML = `
            <span class="category-tag ${doc.category.toLowerCase()}">${escapeHtml(doc.category)}</span>
            <span style="color: var(--text-muted); font-size: 13px;">Article Document · ${wordsCount} words</span>
        `;
        docModalBody.textContent = doc.content;
    } catch (err) {
        docModalBody.textContent = 'Could not load document content.';
    }
}

function showErrorState(msg) {
    resultsSection.style.display = 'block';
    resultsHeader.style.display = 'none';
    resultsContainer.innerHTML = `
        <div class="empty-state" style="border-color: rgba(239, 68, 68, 0.3);">
            <p style="color: #f87171; font-weight: 600;">${escapeHtml(msg)}</p>
        </div>
    `;
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeRegex(string) {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// =============================================================================
// FEATURE 2: LIVE SEARCH ANALYTICS DASHBOARD LOGIC
// =============================================================================

async function reportRoundTripTime(searchId, roundTripMs) {
    if (!searchId || roundTripMs == null) return;
    try {
        await fetch(`${API_BASE}/api/analytics/rtt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ searchId: searchId, roundTripTimeMs: roundTripMs })
        });
    } catch (err) {
        console.warn('Could not report client RTT:', err);
    } finally {
        loadAnalytics();
    }
}

async function handleClearAnalytics() {
    if (!confirm('Are you sure you want to clear all recorded search analytics history? This will reset the dashboard to the empty state.')) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/analytics/clear`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        if (res.ok) {
            loadAnalytics();
        }
    } catch (err) {
        console.error('Failed to clear analytics:', err);
    }
}

async function loadAnalytics() {
    try {
        const res = await fetch(`${API_BASE}/api/analytics`);
        if (!res.ok) return;
        const data = await res.json();
        state.analytics = data;
        renderAnalytics(data);
    } catch (err) {
        console.warn('Could not fetch analytics:', err);
    }
}

function renderAnalytics(data) {
    if (!data) return;

    // 1. Header count badge
    const countBadge = document.getElementById('analyticsCountBadge');
    if (countBadge) {
        countBadge.textContent = data.totalSearches;
        countBadge.style.display = data.totalSearches > 0 ? 'inline-flex' : 'none';
    }

    const emptyStateEl = document.getElementById('analyticsEmptyState');
    const contentEl = document.getElementById('analyticsContent');

    // 2. Empty state vs. Content
    if (!data.totalSearches || data.totalSearches === 0) {
        if (emptyStateEl) emptyStateEl.style.display = 'block';
        if (contentEl) contentEl.style.display = 'none';
        return;
    }

    if (emptyStateEl) emptyStateEl.style.display = 'none';
    if (contentEl) contentEl.style.display = 'block';

    // 3. KPI Cards
    const kpiTotalSearches = document.getElementById('kpiTotalSearches');
    if (kpiTotalSearches) kpiTotalSearches.textContent = Number(data.totalSearches).toLocaleString();

    const kpiAvgExec = document.getElementById('kpiAvgExec');
    if (kpiAvgExec) kpiAvgExec.textContent = `${data.averageExecutionTimeMs} ms`;

    const kpiAvgRtt = document.getElementById('kpiAvgRtt');
    if (kpiAvgRtt) {
        kpiAvgRtt.textContent = data.averageRoundTripTimeMs > 0
            ? `${data.averageRoundTripTimeMs} ms`
            : `${data.averageExecutionTimeMs} ms*`;
    }

    // Top algorithm calculation
    const algos = data.algorithmStats || {};
    let topAlgoKey = '--';
    let topAlgoCount = -1;
    let topAlgoName = 'Coverage-Aware';

    const algoNames = {
        'coverage': 'Coverage-Aware',
        'bm25': 'Okapi BM25',
        'baseline': 'Baseline TF-IDF',
        'compare': '3-Way Comparison'
    };

    for (const [key, obj] of Object.entries(algos)) {
        if (obj.count > topAlgoCount) {
            topAlgoCount = obj.count;
            topAlgoKey = key;
            topAlgoName = algoNames[key] || obj.name;
        }
    }

    const kpiTopAlgo = document.getElementById('kpiTopAlgo');
    if (kpiTopAlgo) kpiTopAlgo.textContent = topAlgoName;

    const kpiTopAlgoMeta = document.getElementById('kpiTopAlgoMeta');
    if (kpiTopAlgoMeta) {
        kpiTopAlgoMeta.textContent = `${topAlgoCount} of ${data.totalSearches} search actions`;
    }

    // 4. Algorithm Distribution Bars & Counts
    const cov = algos.coverage || { count: 0, percentage: 0, avgExecutionTimeMs: 0 };
    const sem = algos.semantic || { count: 0, percentage: 0, avgExecutionTimeMs: 0 };
    const bm = algos.bm25 || { count: 0, percentage: 0, avgExecutionTimeMs: 0 };
    const base = algos.baseline || { count: 0, percentage: 0, avgExecutionTimeMs: 0 };
    const comp = algos.compare || { count: 0, percentage: 0, avgExecutionTimeMs: 0 };

    setDistItem('distCoverageVal', 'distCoverageBar', 'distCoverageLatency', cov, 'Avg backend execution');
    setDistItem('distSemanticVal', 'distSemanticBar', 'distSemanticLatency', sem, 'Avg backend execution');
    setDistItem('distBm25Val', 'distBm25Bar', 'distBm25Latency', bm, 'Avg backend execution');
    setDistItem('distBaselineVal', 'distBaselineBar', 'distBaselineLatency', base, 'Avg backend execution');
    setDistItem('distCompareVal', 'distCompareBar', 'distCompareLatency', comp, 'Avg combined execution');

    // 5. Latency Trend Chart
    renderLatencyChart(data.latencyHistory || []);

    // 6. Recent Searches Table
    renderRecentSearchesTable(data.recentSearches || []);
}

function setDistItem(valId, barId, latId, obj, latPrefix) {
    const valEl = document.getElementById(valId);
    if (valEl) valEl.textContent = `${obj.count} (${obj.percentage}%)`;

    const barEl = document.getElementById(barId);
    if (barEl) barEl.style.width = `${Math.min(100, Math.max(0, obj.percentage))}%`;

    const latEl = document.getElementById(latId);
    if (latEl) latEl.textContent = `${latPrefix}: ${obj.avgExecutionTimeMs} ms`;
}

function renderLatencyChart(history) {
    const svg = document.getElementById('latencyChartSvg');
    const tooltip = document.getElementById('chartTooltip');
    const container = document.getElementById('chartContainer');
    if (!svg || !container) return;

    if (!history || history.length === 0) {
        svg.innerHTML = `
            <text x="250" y="90" text-anchor="middle" fill="#64748b" font-size="12" font-family="sans-serif">
                No latency history recorded yet
            </text>
        `;
        return;
    }

    const W = 500;
    const H = 180;
    const padX = 35;
    const padY = 25;
    const chartW = W - padX * 2;
    const chartH = H - padY * 2;

    // Find max latency for scaling
    let maxVal = 10;
    history.forEach(item => {
        if (item.executionTimeMs > maxVal) maxVal = item.executionTimeMs;
        if (item.roundTripTimeMs > maxVal) maxVal = item.roundTripTimeMs;
    });
    // Add 20% headroom
    maxVal = Math.ceil(maxVal * 1.2);

    let svgHtml = '';

    // Draw horizontal grid lines & labels (3 levels: 0%, 50%, 100%)
    const gridLevels = [0, maxVal / 2, maxVal];
    gridLevels.forEach(val => {
        const y = padY + chartH - (val / maxVal) * chartH;
        svgHtml += `
            <line x1="${padX}" y1="${y}" x2="${W - padX}" y2="${y}" stroke="rgba(255,255,255,0.06)" stroke-dasharray="3,3" />
            <text x="${padX - 8}" y="${y + 3}" text-anchor="end" fill="#64748b" font-size="9" font-family="monospace">
                ${Math.round(val)}ms
            </text>
        `;
    });

    // Zero baseline
    svgHtml += `<line x1="${padX}" y1="${padY + chartH}" x2="${W - padX}" y2="${padY + chartH}" stroke="rgba(255,255,255,0.15)" />`;

    const count = history.length;
    const stepX = count > 1 ? chartW / (count - 1) : chartW / 2;

    // Calculate points
    const execPoints = [];
    const rttPoints = [];

    history.forEach((item, idx) => {
        const x = count === 1 ? padX + chartW / 2 : padX + idx * stepX;
        const yExec = padY + chartH - (Math.max(0, item.executionTimeMs) / maxVal) * chartH;
        execPoints.push({ x, y: yExec, item, idx });

        const rttVal = item.roundTripTimeMs > 0 ? item.roundTripTimeMs : item.executionTimeMs;
        const yRtt = padY + chartH - (Math.max(0, rttVal) / maxVal) * chartH;
        rttPoints.push({ x, y: yRtt, item, idx });
    });

    // Build polylines
    if (count > 1) {
        const rttPath = rttPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
        const execPath = execPoints.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');

        // RTT line (purple)
        svgHtml += `<polyline fill="none" stroke="#c084fc" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" points="${rttPath}" />`;
        // Exec line (cyan/blue)
        svgHtml += `<polyline fill="none" stroke="#38bdf8" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" points="${execPath}" />`;
    }

    // Render interactive data circles
    rttPoints.forEach(p => {
        svgHtml += `
            <circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="4" fill="#c084fc" stroke="#0f172a" stroke-width="1.5"
                    class="chart-point rtt-point" data-idx="${p.idx}" style="cursor: pointer;" />
        `;
    });

    execPoints.forEach(p => {
        svgHtml += `
            <circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="4.5" fill="#38bdf8" stroke="#0f172a" stroke-width="1.5"
                    class="chart-point exec-point" data-idx="${p.idx}" style="cursor: pointer;" />
        `;
    });

    svg.innerHTML = svgHtml;

    // Setup interactive tooltip
    svg.querySelectorAll('.chart-point').forEach(circle => {
        circle.addEventListener('mouseenter', (e) => {
            const idx = parseInt(circle.getAttribute('data-idx'), 10);
            const item = history[idx];
            if (!item || !tooltip) return;

            const rttText = item.roundTripTimeMs > 0 ? `${item.roundTripTimeMs} ms` : 'Measuring...';
            tooltip.innerHTML = `
                <div style="font-weight: 700; color: #ffffff; margin-bottom: 3px;">"${escapeHtml(item.query)}"</div>
                <div style="color: #38bdf8;">Backend Execution: <strong>${item.executionTimeMs} ms</strong></div>
                <div style="color: #c084fc;">Browser Round-Trip: <strong>${rttText}</strong></div>
                <div style="font-size: 10px; color: #94a3b8; margin-top: 2px;">Algorithm: ${escapeHtml(item.algoLabel || item.algo)}</div>
            `;
            tooltip.style.display = 'block';

            const rect = container.getBoundingClientRect();
            const circleRect = circle.getBoundingClientRect();
            const left = circleRect.left - rect.left + circleRect.width / 2;
            const top = circleRect.top - rect.top;

            tooltip.style.left = `${left}px`;
            tooltip.style.top = `${top}px`;
        });

        circle.addEventListener('mouseleave', () => {
            if (tooltip) tooltip.style.display = 'none';
        });
    });
}

function renderRecentSearchesTable(searches) {
    const tbody = document.getElementById('recentSearchesTableBody');
    const countEl = document.getElementById('tableRecordCount');
    if (!tbody) return;

    if (countEl) {
        countEl.textContent = `Showing last ${searches.length} recorded search actions`;
    }

    if (!searches || searches.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 24px; color: var(--text-muted);">
                    No searches recorded in current session.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = '';

    searches.forEach((r, idx) => {
        const tr = document.createElement('tr');

        let algoClass = 'tbl-algo-coverage';
        let algoBadgeText = 'Coverage-Aware';

        if (r.isComparison || r.algo === 'compare') {
            algoClass = 'tbl-algo-compare';
            algoBadgeText = '⚡ 3-Way Comparison';
        } else if (r.algo === 'bm25') {
            algoClass = 'tbl-algo-bm25';
            algoBadgeText = 'Okapi BM25';
        } else if (r.algo === 'baseline') {
            algoClass = 'tbl-algo-baseline';
            algoBadgeText = 'Baseline TF-IDF';
        }

        const categoryClass = (r.category || 'all').toLowerCase();
        const rttHtml = r.roundTripTimeMs > 0
            ? `<span class="tbl-time-badge tbl-rtt-badge">${r.roundTripTimeMs} ms</span>`
            : `<span class="tbl-time-pending">—</span>`;

        tr.innerHTML = `
            <td style="color: var(--text-muted); font-family: var(--font-mono);">${idx + 1}</td>
            <td>
                <span class="tbl-query-link" data-query="${escapeHtml(r.query)}" data-algo="${escapeHtml(r.algo)}" title="Click to repeat search">
                    ${escapeHtml(r.query)}
                </span>
            </td>
            <td>
                <span class="tbl-algo-pill ${algoClass}">${escapeHtml(algoBadgeText)}</span>
            </td>
            <td>
                <span class="category-tag ${categoryClass}">${escapeHtml(r.category || 'all')}</span>
            </td>
            <td>
                <span class="tbl-time-badge tbl-exec-badge">${r.executionTimeMs} ms</span>
            </td>
            <td>
                ${rttHtml}
            </td>
            <td style="font-family: var(--font-mono); font-size: 11px;">
                ${Number(r.totalMatches || 0).toLocaleString()}
            </td>
            <td>
                <span class="tbl-timestamp">${escapeHtml(r.formattedTime || '')}</span>
            </td>
        `;

        // Click on query repeats the search
        const link = tr.querySelector('.tbl-query-link');
        if (link) {
            link.addEventListener('click', () => {
                queryInput.value = r.query;
                clearBtn.style.display = 'flex';
                if (r.isComparison || r.algo === 'compare') {
                    switchToCompareMode();
                    performComparisonSearch(r.query);
                } else {
                    switchToSingleMode(r.algo || 'coverage');
                    performSearch(r.query);
                }
            });
        }

        tbody.appendChild(tr);
    });
}

// =============================================================================
// FEATURE 3: RESEARCH WORKSPACE CLIENT APPLICATION LOGIC
// =============================================================================

function isDocumentBookmarked(docId) {
    if (!state.workspace || !state.workspace.bookmarks || !docId) return false;
    return state.workspace.bookmarks.some(b => b.docId === String(docId));
}

async function loadWorkspace() {
    try {
        const res = await fetch(`${API_BASE}/api/workspace`);
        if (!res.ok) return;
        const data = await res.json();
        state.workspace = data;
        renderWorkspace();
        updateAllBookmarkButtons();
    } catch (err) {
        console.warn('Could not load workspace:', err);
    }
}

function updateAllBookmarkButtons() {
    document.querySelectorAll('.btn-bookmark-action').forEach(btn => {
        const docId = btn.getAttribute('data-doc-id');
        const saved = isDocumentBookmarked(docId);
        if (saved) {
            btn.classList.add('saved');
            const svg = btn.querySelector('svg');
            if (svg) svg.setAttribute('fill', 'currentColor');
            const label = btn.querySelector('.bm-label');
            if (label) label.textContent = 'Bookmarked';
        } else {
            btn.classList.remove('saved');
            const svg = btn.querySelector('svg');
            if (svg) svg.setAttribute('fill', 'none');
            const label = btn.querySelector('.bm-label');
            if (label) label.textContent = 'Bookmark';
        }
    });
}

async function toggleBookmark(docId, category) {
    if (!docId) return;
    const currentlySaved = isDocumentBookmarked(docId);

    try {
        if (currentlySaved) {
            await fetch(`${API_BASE}/api/workspace`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'removeBookmark', docId: String(docId) })
            });
        } else {
            await fetch(`${API_BASE}/api/workspace`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    action: 'addBookmark',
                    docId: String(docId),
                    category: category || 'all',
                    collectionId: 'default'
                })
            });
        }
        await loadWorkspace();
    } catch (err) {
        console.error('Bookmark toggle failed:', err);
    }
}

function switchWorkspaceTab(tab) {
    state.activeWsTab = tab;
    const tabBookmarks = document.getElementById('wsTabBookmarks');
    const tabSearches = document.getElementById('wsTabSearches');
    const panelBookmarks = document.getElementById('wsBookmarksPanel');
    const panelSearches = document.getElementById('wsSearchesPanel');

    if (tab === 'bookmarks') {
        if (tabBookmarks) tabBookmarks.classList.add('active');
        if (tabSearches) tabSearches.classList.remove('active');
        if (panelBookmarks) panelBookmarks.style.display = 'block';
        if (panelSearches) panelSearches.style.display = 'none';
    } else {
        if (tabBookmarks) tabBookmarks.classList.remove('active');
        if (tabSearches) tabSearches.classList.add('active');
        if (panelBookmarks) panelBookmarks.style.display = 'none';
        if (panelSearches) panelSearches.style.display = 'block';
    }
}

function renderWorkspace() {
    const ws = state.workspace;
    if (!ws) return;

    // Badges & Counters
    const navBadge = document.getElementById('workspaceCountBadge');
    if (navBadge) {
        navBadge.textContent = ws.totalBookmarks;
        navBadge.style.display = ws.totalBookmarks > 0 ? 'inline-flex' : 'none';
    }

    const bmCountEl = document.getElementById('wsBookmarkCount');
    if (bmCountEl) bmCountEl.textContent = ws.totalBookmarks;

    const ssCountEl = document.getElementById('wsSearchCount');
    if (ssCountEl) ssCountEl.textContent = ws.totalSavedSearches;

    // 1. Render Collection Filter Pills
    renderCollectionPills(ws.collections || [], ws.bookmarks || []);

    // 2. Render Bookmarked Documents
    renderBookmarkedCards(ws.bookmarks || [], ws.collections || []);

    // 3. Render Saved Searches
    renderSavedSearchesList(ws.savedSearches || []);
}

function renderCollectionPills(collections, bookmarks) {
    const listEl = document.getElementById('collectionsListPills');
    if (!listEl) return;
    listEl.innerHTML = '';

    // "All Collections" Pill
    const allPill = document.createElement('button');
    allPill.type = 'button';
    allPill.className = `ws-col-pill ${state.selectedCollectionFilter === 'all' ? 'active' : ''}`;
    allPill.innerHTML = `All Bookmarks <span class="col-pill-badge">${bookmarks.length}</span>`;
    allPill.addEventListener('click', () => {
        state.selectedCollectionFilter = 'all';
        renderWorkspace();
    });
    listEl.appendChild(allPill);

    // Each individual collection
    collections.forEach(col => {
        const count = bookmarks.filter(b => b.collectionId === col.id).length;
        const pill = document.createElement('div');
        pill.className = `ws-col-pill ${state.selectedCollectionFilter === col.id ? 'active' : ''}`;

        const isDefault = col.id === 'default';
        const actionButtons = !isDefault ? `
            <span class="col-pill-actions">
                <button type="button" class="btn-col-action btn-col-edit" title="Rename Collection">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                </button>
                <button type="button" class="btn-col-action btn-col-delete" title="Delete Collection">✕</button>
            </span>
        ` : '';

        pill.innerHTML = `
            <span class="col-pill-label">${escapeHtml(col.name)}</span>
            <span class="col-pill-badge">${count}</span>
            ${actionButtons}
        `;

        pill.querySelector('.col-pill-label').addEventListener('click', () => {
            state.selectedCollectionFilter = col.id;
            renderWorkspace();
        });

        if (!isDefault) {
            const editBtn = pill.querySelector('.btn-col-edit');
            if (editBtn) {
                editBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    promptRenameCollection(col);
                });
            }

            const delBtn = pill.querySelector('.btn-col-delete');
            if (delBtn) {
                delBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    handleDeleteCollection(col);
                });
            }
        }

        listEl.appendChild(pill);
    });
}

function promptRenameCollection(col) {
    state.editingCollectionId = col.id;
    const formCard = document.getElementById('collectionFormCard');
    const titleEl = document.getElementById('colFormTitle');
    const inputEl = document.getElementById('collectionNameInput');
    if (titleEl) titleEl.textContent = `Rename Collection: "${col.name}"`;
    if (inputEl) inputEl.value = col.name;
    if (formCard) formCard.style.display = 'block';
    if (inputEl) inputEl.focus();
}

async function handleSaveCollectionSubmit() {
    const inputEl = document.getElementById('collectionNameInput');
    const formCard = document.getElementById('collectionFormCard');
    if (!inputEl) return;
    const name = inputEl.value.trim();
    if (!name) {
        alert('Please enter a collection name.');
        return;
    }

    try {
        if (state.editingCollectionId) {
            await fetch(`${API_BASE}/api/workspace`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'renameCollection', id: state.editingCollectionId, name: name })
            });
        } else {
            await fetch(`${API_BASE}/api/workspace`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: 'createCollection', name: name })
            });
        }
        if (formCard) formCard.style.display = 'none';
        state.editingCollectionId = null;
        await loadWorkspace();
    } catch (err) {
        console.error('Collection save failed:', err);
    }
}

async function handleDeleteCollection(col) {
    if (!confirm(`Delete collection "${col.name}"? Any bookmarked documents inside will be safely moved to General Research.`)) {
        return;
    }
    try {
        await fetch(`${API_BASE}/api/workspace`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'deleteCollection', id: col.id })
        });
        if (state.selectedCollectionFilter === col.id) {
            state.selectedCollectionFilter = 'all';
        }
        await loadWorkspace();
    } catch (err) {
        console.error('Delete collection failed:', err);
    }
}

function renderBookmarkedCards(bookmarks, collections) {
    const container = document.getElementById('wsBookmarksContainer');
    const emptyEl = document.getElementById('wsBookmarksEmpty');
    if (!container) return;

    // Filter by selected collection if active
    let filtered = bookmarks;
    if (state.selectedCollectionFilter !== 'all') {
        filtered = bookmarks.filter(b => b.collectionId === state.selectedCollectionFilter);
    }

    if (filtered.length === 0) {
        if (emptyEl) emptyEl.style.display = 'block';
        container.innerHTML = '';
        return;
    }

    if (emptyEl) emptyEl.style.display = 'none';
    container.innerHTML = '';

    filtered.forEach(bm => {
        const card = document.createElement('div');
        card.className = 'ws-doc-card';

        const categoryClass = (bm.category || 'all').toLowerCase();
        const isEditingNotes = state.editingNoteDocId === bm.docId;

        // Build collection options
        const collectionOptionsHtml = collections.map(c => `
            <option value="${escapeHtml(c.id)}" ${c.id === bm.collectionId ? 'selected' : ''}>
                ${escapeHtml(c.name)}
            </option>
        `).join('');

        // Notes HTML
        let notesHtml = '';
        if (isEditingNotes) {
            notesHtml = `
                <div class="ws-notes-block">
                    <div class="ws-notes-header">
                        <span class="ws-notes-label">Editing Personal Note:</span>
                    </div>
                    <textarea class="ws-notes-textarea" id="noteInput_${escapeHtml(bm.docId)}" placeholder="Write personal findings, study notes, or citations...">${escapeHtml(bm.notes || '')}</textarea>
                    <div class="ws-notes-edit-actions">
                        <button type="button" class="btn-secondary btn-cancel-note" style="padding: 4px 10px; font-size: 11px;">Cancel</button>
                        <button type="button" class="btn-primary btn-save-note" style="padding: 4px 12px; font-size: 11px;">Save Note</button>
                    </div>
                </div>
            `;
        } else if (bm.notes && bm.notes.trim().length > 0) {
            notesHtml = `
                <div class="ws-notes-block">
                    <div class="ws-notes-header">
                        <span class="ws-notes-label">
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                            </svg>
                            Personal Research Note
                        </span>
                        <div style="display: flex; gap: 6px;">
                            <button type="button" class="btn-secondary btn-edit-note" style="padding: 2px 8px; font-size: 11px;">Edit</button>
                            <button type="button" class="btn-secondary btn-danger-subtle btn-del-note" style="padding: 2px 8px; font-size: 11px;">Clear</button>
                        </div>
                    </div>
                    <div class="ws-notes-content">"${escapeHtml(bm.notes)}"</div>
                </div>
            `;
        } else {
            notesHtml = `
                <div class="ws-notes-block" style="background: rgba(255,255,255,0.015);">
                    <div class="ws-notes-header">
                        <span class="ws-notes-empty-hint">No notes recorded yet.</span>
                        <button type="button" class="btn-secondary btn-edit-note" style="padding: 2px 10px; font-size: 11px;">+ Add Note</button>
                    </div>
                </div>
            `;
        }

        card.innerHTML = `
            <div class="ws-card-top">
                <div class="ws-card-id-row">
                    <span class="doc-id">Doc #${escapeHtml(bm.docId)}.txt</span>
                    <span class="category-tag ${categoryClass}">${escapeHtml(bm.category)}</span>
                </div>
                <div class="ws-card-collection-row">
                    <label>Collection:</label>
                    <select class="ws-col-select" data-doc-id="${escapeHtml(bm.docId)}">
                        ${collectionOptionsHtml}
                    </select>
                </div>
            </div>

            <div class="ws-doc-title">${escapeHtml(bm.title)}</div>
            <div class="ws-doc-snippet">${escapeHtml(bm.snippet || '')}</div>

            ${notesHtml}

            <div class="ws-card-bottom">
                <div class="ws-card-actions-left">
                    <button type="button" class="btn-read-ws" data-doc-id="${escapeHtml(bm.docId)}" data-doc-cat="${escapeHtml(bm.category)}">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                            <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                        </svg>
                        Read Full Article
                    </button>
                    <button type="button" class="btn-ws-unbookmark" data-doc-id="${escapeHtml(bm.docId)}">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"></path>
                        </svg>
                        Remove
                    </button>
                </div>
                <div class="ws-card-date">Saved ${escapeHtml(bm.formattedDate || '')}</div>
            </div>
        `;

        // Wire event listeners on card
        card.querySelector('.btn-read-ws').addEventListener('click', () => {
            openDocumentModal(bm.docId, bm.category);
        });

        card.querySelector('.btn-ws-unbookmark').addEventListener('click', async () => {
            if (confirm(`Remove document #${bm.docId} from your workspace?`)) {
                await toggleBookmark(bm.docId, bm.category);
            }
        });

        const selectEl = card.querySelector('.ws-col-select');
        if (selectEl) {
            selectEl.addEventListener('change', async (e) => {
                const newColId = e.target.value;
                try {
                    await fetch(`${API_BASE}/api/workspace`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ action: 'assignCollection', docId: bm.docId, collectionId: newColId })
                    });
                    await loadWorkspace();
                } catch (err) {
                    console.error('Assign collection failed:', err);
                }
            });
        }

        const editNoteBtn = card.querySelector('.btn-edit-note');
        if (editNoteBtn) {
            editNoteBtn.addEventListener('click', () => {
                state.editingNoteDocId = bm.docId;
                renderWorkspace();
            });
        }

        const delNoteBtn = card.querySelector('.btn-del-note');
        if (delNoteBtn) {
            delNoteBtn.addEventListener('click', async () => {
                try {
                    await fetch(`${API_BASE}/api/workspace`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ action: 'updateNotes', docId: bm.docId, notes: '' })
                    });
                    await loadWorkspace();
                } catch (err) {
                    console.error('Delete note failed:', err);
                }
            });
        }

        const cancelNoteBtn = card.querySelector('.btn-cancel-note');
        if (cancelNoteBtn) {
            cancelNoteBtn.addEventListener('click', () => {
                state.editingNoteDocId = null;
                renderWorkspace();
            });
        }

        const saveNoteBtn = card.querySelector('.btn-save-note');
        if (saveNoteBtn) {
            saveNoteBtn.addEventListener('click', async () => {
                const textarea = card.querySelector(`#noteInput_${bm.docId}`);
                const noteVal = textarea ? textarea.value.trim() : '';
                try {
                    await fetch(`${API_BASE}/api/workspace`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ action: 'updateNotes', docId: bm.docId, notes: noteVal })
                    });
                    state.editingNoteDocId = null;
                    await loadWorkspace();
                } catch (err) {
                    console.error('Save note failed:', err);
                }
            });
        }

        container.appendChild(card);
    });
}

function renderSavedSearchesList(searches) {
    const container = document.getElementById('wsSearchesContainer');
    const emptyEl = document.getElementById('wsSearchesEmpty');
    if (!container) return;

    if (!searches || searches.length === 0) {
        if (emptyEl) emptyEl.style.display = 'block';
        container.innerHTML = '';
        return;
    }

    if (emptyEl) emptyEl.style.display = 'none';
    container.innerHTML = '';

    searches.forEach(ss => {
        const item = document.createElement('div');
        item.className = 'saved-search-item';

        const algoLabels = {
            'coverage': 'Coverage-Aware TF-IDF',
            'bm25': 'Okapi BM25',
            'baseline': 'Baseline TF-IDF',
            'compare': '⚡ 3-Way Comparison Lab'
        };
        const algoText = algoLabels[ss.algo] || ss.algo;

        item.innerHTML = `
            <div class="saved-search-info">
                <span class="saved-search-query">"${escapeHtml(ss.query)}"</span>
                <span class="tbl-algo-pill tbl-algo-${escapeHtml(ss.algo)}">${escapeHtml(algoText)}</span>
                <span class="category-tag ${(ss.category || 'all').toLowerCase()}">${escapeHtml(ss.category)}</span>
                <span style="font-size: 11px; color: var(--text-muted); font-family: var(--font-mono);">Saved ${escapeHtml(ss.formattedDate || '')}</span>
            </div>
            <div class="saved-search-actions">
                <button type="button" class="btn-run-search" data-query="${escapeHtml(ss.query)}" data-algo="${escapeHtml(ss.algo)}" data-cat="${escapeHtml(ss.category)}">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polygon points="5 3 19 12 5 21 5 3"></polygon>
                    </svg>
                    Run Search
                </button>
                <button type="button" class="btn-del-search" data-id="${escapeHtml(ss.id)}" title="Delete saved search">
                    ✕
                </button>
            </div>
        `;

        item.querySelector('.btn-run-search').addEventListener('click', () => {
            queryInput.value = ss.query;
            clearBtn.style.display = 'flex';

            // Set category
            state.category = ss.category || 'all';
            document.querySelectorAll('.category-pill').forEach(p => {
                if (p.dataset.category === state.category) p.classList.add('active');
                else p.classList.remove('active');
            });

            if (ss.algo === 'compare') {
                switchToCompareMode();
                performComparisonSearch(ss.query);
            } else {
                switchToSingleMode(ss.algo || 'coverage');
                performSearch(ss.query);
            }

            // Scroll up to results
            resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });

        item.querySelector('.btn-del-search').addEventListener('click', async () => {
            try {
                await fetch(`${API_BASE}/api/workspace`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ action: 'deleteSearch', id: ss.id })
                });
                await loadWorkspace();
            } catch (err) {
                console.error('Delete saved search failed:', err);
            }
        });

        container.appendChild(item);
    });
}

async function handleSaveCurrentQuery(triggerBtn) {
    const q = queryInput.value.trim() || state.query;
    if (!q) {
        alert('Please enter or execute a search query before saving.');
        return;
    }

    const algo = state.mode === 'compare' ? 'compare' : (state.algo || 'coverage');
    const cat = state.category || 'all';

    try {
        const res = await fetch(`${API_BASE}/api/workspace`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'saveSearch', query: q, algo: algo, category: cat })
        });
        if (res.ok) {
            if (triggerBtn) {
                const originalHtml = triggerBtn.innerHTML;
                triggerBtn.innerHTML = `✓ Saved!`;
                triggerBtn.style.borderColor = '#10b981';
                triggerBtn.style.color = '#34d399';
                setTimeout(() => {
                    triggerBtn.innerHTML = originalHtml;
                    triggerBtn.style.borderColor = '';
                    triggerBtn.style.color = '';
                }, 2000);
            }
            await loadWorkspace();
        }
    } catch (err) {
        console.error('Save query failed:', err);
    }
}

async function handleClearWorkspace() {
    if (!confirm('Are you sure you want to clear your entire Research Workspace? All bookmarks, notes, and saved queries will be removed.')) {
        return;
    }
    try {
        await fetch(`${API_BASE}/api/workspace`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'clear' })
        });
        state.selectedCollectionFilter = 'all';
        await loadWorkspace();
    } catch (err) {
        console.error('Clear workspace failed:', err);
    }
}

// =============================================================================
// GOOGLE COMPARISON LAB LOGIC & EVALUATION
// =============================================================================

let currentGoogleComparisonQuery = '';
let currentGoogleUrl = '';

function initGoogleComparisonLab() {
    const googleComparisonNavBtn = document.getElementById('googleComparisonNavBtn');
    if (googleComparisonNavBtn) {
        googleComparisonNavBtn.addEventListener('click', () => {
            const el = document.getElementById('googleComparisonLab');
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    }

    const btnSearchBoth = document.getElementById('btnSearchBoth');
    const gcompQueryInput = document.getElementById('gcompQueryInput');
    const btnReopenGoogle = document.getElementById('btnReopenGoogle');
    const evalRelevantCount = document.getElementById('evalRelevantCount');
    const refreshGcompBtn = document.getElementById('refreshGcompBtn');
    const clearGcompBtn = document.getElementById('clearGcompBtn');
    const gcompEvalForm = document.getElementById('gcompEvalForm');

    if (btnSearchBoth) {
        btnSearchBoth.addEventListener('click', performGoogleComparisonSearch);
    }

    if (gcompQueryInput) {
        gcompQueryInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                performGoogleComparisonSearch();
            }
        });
    }

    if (btnReopenGoogle) {
        btnReopenGoogle.addEventListener('click', () => {
            if (currentGoogleUrl) {
                window.open(currentGoogleUrl, '_blank', 'noopener,noreferrer');
            }
        });
    }

    if (evalRelevantCount) {
        evalRelevantCount.addEventListener('change', updateCalcPrecision);
        updateCalcPrecision();
    }

    if (refreshGcompBtn) {
        refreshGcompBtn.addEventListener('click', loadGoogleComparisonHistory);
    }

    if (clearGcompBtn) {
        clearGcompBtn.addEventListener('click', handleClearGoogleComparisonHistory);
    }

    if (gcompEvalForm) {
        gcompEvalForm.addEventListener('submit', handleSaveComparisonObservation);
    }
}

function updateCalcPrecision() {
    const evalRelevantCount = document.getElementById('evalRelevantCount');
    const calcPrecisionBadge = document.getElementById('calcPrecisionBadge');
    if (!evalRelevantCount || !calcPrecisionBadge) return;

    const count = parseInt(evalRelevantCount.value, 10) || 0;
    const pct = ((count / 5.0) * 100.0).toFixed(1);
    calcPrecisionBadge.textContent = `${pct}% (${count}/5)`;
}

async function performGoogleComparisonSearch() {
    const gcompQueryInput = document.getElementById('gcompQueryInput');
    const gcompAlgoSelect = document.getElementById('gcompAlgoSelect');
    const gcompCategorySelect = document.getElementById('gcompCategorySelect');
    const btnSearchBoth = document.getElementById('btnSearchBoth');
    const gcompResultsArea = document.getElementById('gcompResultsArea');
    const localResultsList = document.getElementById('localResultsList');
    const localMetaBadge = document.getElementById('localMetaBadge');
    const googleTargetUrl = document.getElementById('googleTargetUrl');
    const googleTabStatus = document.getElementById('googleTabStatus');
    const evalQueryInput = document.getElementById('evalQueryInput');

    const query = gcompQueryInput ? gcompQueryInput.value.trim() : '';
    if (!query) {
        if (gcompQueryInput) {
            gcompQueryInput.focus();
            gcompQueryInput.style.borderColor = '#ef4444';
            setTimeout(() => { gcompQueryInput.style.borderColor = ''; }, 2000);
        }
        return;
    }

    currentGoogleComparisonQuery = query;
    const algo = gcompAlgoSelect ? gcompAlgoSelect.value : 'coverage';
    const cat = gcompCategorySelect ? gcompCategorySelect.value : 'all';

    // 1. Launch Google Search in a new browser tab (No scraping, no automated querying)
    currentGoogleUrl = `https://www.google.com/search?q=${encodeURIComponent(query)}`;
    window.open(currentGoogleUrl, '_blank', 'noopener,noreferrer');

    if (googleTargetUrl) {
        googleTargetUrl.innerHTML = `<a href="${currentGoogleUrl}" target="_blank" rel="noopener noreferrer" style="color: #60a5fa; text-decoration: underline;">${escapeHtml(currentGoogleUrl)}</a>`;
    }
    if (googleTabStatus) {
        googleTabStatus.innerHTML = `Opened Google search for <strong>"${escapeHtml(query)}"</strong> in an external browser tab.`;
    }
    if (evalQueryInput) {
        evalQueryInput.value = query;
    }

    // 2. Execute Local Java Search Engine API
    if (gcompResultsArea) gcompResultsArea.style.display = 'grid';
    if (localResultsList) {
        localResultsList.innerHTML = `
            <div class="comp-skeleton-card">
                <div class="skeleton-shimmer skeleton-top"></div>
                <div class="skeleton-shimmer skeleton-meta"></div>
                <div class="skeleton-shimmer skeleton-body"></div>
            </div>
        `;
    }
    if (localMetaBadge) localMetaBadge.textContent = 'Searching 50,000-doc corpus...';
    if (btnSearchBoth) btnSearchBoth.disabled = true;

    const start = performance.now();
    try {
        const url = `${API_BASE}/api/search?q=${encodeURIComponent(query)}&category=${encodeURIComponent(cat)}&algo=${encodeURIComponent(algo)}&limit=5`;
        const res = await fetch(url);
        const clientLatency = Math.round(performance.now() - start);

        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        renderLocalGcompResults(data, algo, cat, clientLatency);

        // Pre-fill latency into evaluation form if empty
        const evalLatencyInput = document.getElementById('evalLatencyInput');
        if (evalLatencyInput && !evalLatencyInput.value) {
            evalLatencyInput.value = clientLatency;
        }
    } catch (err) {
        console.error('Local comparison search failed:', err);
        if (localResultsList) {
            localResultsList.innerHTML = `
                <div style="padding: 16px; color: #f87171; background: rgba(239, 68, 68, 0.1); border-radius: var(--radius-sm);">
                    <strong>Local Search Failed:</strong> ${escapeHtml(err.message)}
                </div>
            `;
        }
    } finally {
        if (btnSearchBoth) btnSearchBoth.disabled = false;
        if (gcompResultsArea) {
            gcompResultsArea.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }
}

function renderLocalGcompResults(data, algo, cat, clientLatency) {
    const localResultsList = document.getElementById('localResultsList');
    const localMetaBadge = document.getElementById('localMetaBadge');
    if (!localResultsList) return;

    const algoLabels = {
        'coverage': 'Coverage-Aware TF-IDF',
        'bm25': 'Okapi BM25',
        'baseline': 'Baseline TF-IDF',
        'semantic': 'Semantic Search (LSA 128-D)'
    };
    const algoLabel = algoLabels[algo] || algo;

    if (localMetaBadge) {
        localMetaBadge.textContent = `${algoLabel} · Backend: ${data.executionTimeMs}ms · RTT: ${clientLatency}ms · Matches: ${data.totalMatches}`;
    }

    const results = data.results || [];
    if (results.length === 0) {
        localResultsList.innerHTML = `
            <div style="padding: 20px; text-align: center; color: var(--text-muted); font-size: 13px;">
                No matching documents found in the local 50,000-document corpus for "${escapeHtml(data.query)}".
            </div>
        `;
        return;
    }

    localResultsList.innerHTML = results.map(doc => {
        const title = doc.fileName || `Doc #${doc.id}`;
        const scoreLabel = doc.scoreLabel || 'Score';
        const scoreVal = doc.score || '0.000000';
        const category = doc.category || 'general';

        let extraMetaHtml = '';
        if (algo === 'semantic' && doc.topConcepts && doc.topConcepts.length > 0) {
            extraMetaHtml = `
                <div class="gcomp-card-tags" style="margin-top: 6px;">
                    ${doc.topConcepts.slice(0, 2).map(c => `<span class="gcomp-tag" style="color: #c084fc; background: rgba(168, 85, 247, 0.12);">🧠 ${escapeHtml(c)}</span>`).join('')}
                    ${(doc.relatedKeywords || []).slice(0, 3).map(k => `<span class="gcomp-tag">🔗 ${escapeHtml(k)}</span>`).join('')}
                </div>
            `;
        } else if (doc.coveragePercentage) {
            extraMetaHtml = `
                <div class="gcomp-card-tags" style="margin-top: 6px;">
                    <span class="gcomp-tag" style="color: #38bdf8; background: rgba(56, 189, 248, 0.12);">Coverage: ${doc.coveragePercentage}% (${doc.matchedTermsCount}/${doc.totalValidTerms} terms)</span>
                </div>
            `;
        }

        return `
            <div class="gcomp-result-card">
                <div class="gcomp-card-top">
                    <span class="gcomp-card-rank">Rank #${doc.rank}</span>
                    <span class="gcomp-card-score">${scoreLabel}: ${scoreVal}</span>
                </div>
                <div class="gcomp-card-title">${escapeHtml(title)}</div>
                <div class="gcomp-card-snippet">${escapeHtml(doc.snippet || 'No preview available.')}</div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px;">
                    <span class="category-badge cat-${category.toLowerCase()}">${category}</span>
                    <button type="button" class="btn-read-doc" onclick="openDocumentModal('${escapeHtml(doc.id)}', '${escapeHtml(category)}')" style="background: none; border: none; color: #60a5fa; font-size: 11px; cursor: pointer; text-decoration: underline;">
                        Read Full
                    </button>
                </div>
                ${extraMetaHtml}
            </div>
        `;
    }).join('');
}

async function handleSaveComparisonObservation(e) {
    e.preventDefault();
    const saveObservationMsg = document.getElementById('saveObservationMsg');
    const evalQueryInput = document.getElementById('evalQueryInput');
    const evalEngineSelect = document.getElementById('evalEngineSelect');
    const evalRelevantCount = document.getElementById('evalRelevantCount');
    const evalLatencyInput = document.getElementById('evalLatencyInput');
    const evalNotesInput = document.getElementById('evalNotesInput');
    const gcompAlgoSelect = document.getElementById('gcompAlgoSelect');
    const gcompCategorySelect = document.getElementById('gcompCategorySelect');

    const query = evalQueryInput ? evalQueryInput.value.trim() : '';
    if (!query) {
        if (evalQueryInput) evalQueryInput.focus();
        return;
    }

    const engine = evalEngineSelect ? evalEngineSelect.value : 'local';
    const relevantCount = evalRelevantCount ? parseInt(evalRelevantCount.value, 10) : 0;
    const responseTimeMs = evalLatencyInput ? (parseInt(evalLatencyInput.value, 10) || 0) : 0;
    const notes = evalNotesInput ? evalNotesInput.value.trim() : '';
    const localAlgo = gcompAlgoSelect ? gcompAlgoSelect.value : 'coverage';
    const category = gcompCategorySelect ? gcompCategorySelect.value : 'all';

    try {
        const res = await fetch(`${API_BASE}/api/google-eval`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                query,
                engine,
                localAlgo,
                category,
                relevantCount,
                responseTimeMs,
                notes
            })
        });

        if (res.ok) {
            if (saveObservationMsg) {
                saveObservationMsg.textContent = '✓ Observation saved successfully!';
                saveObservationMsg.className = 'save-msg success';
                setTimeout(() => { if (saveObservationMsg) saveObservationMsg.textContent = ''; }, 3000);
            }
            if (evalNotesInput) evalNotesInput.value = '';
            await loadGoogleComparisonHistory();
        } else {
            throw new Error(`Server returned HTTP ${res.status}`);
        }
    } catch (err) {
        console.error('Save observation failed:', err);
        if (saveObservationMsg) {
            saveObservationMsg.textContent = 'Failed to save observation: ' + err.message;
            saveObservationMsg.className = 'save-msg error';
        }
    }
}

async function loadGoogleComparisonHistory() {
    const histTotalObs = document.getElementById('histTotalObs');
    const histLocalP5 = document.getElementById('histLocalP5');
    const histGoogleP5 = document.getElementById('histGoogleP5');
    const gcompHistoryTableBody = document.getElementById('gcompHistoryTableBody');
    const gcompHistoryEmpty = document.getElementById('gcompHistoryEmpty');

    try {
        const res = await fetch(`${API_BASE}/api/google-eval`);
        if (!res.ok) return;

        const data = await res.json();
        const total = data.totalObservations || 0;

        if (histTotalObs) histTotalObs.textContent = total;
        if (histLocalP5) {
            histLocalP5.textContent = data.localObservationsCount > 0
                ? `${(data.localAvgPrecisionAt5 * 100).toFixed(1)}% (${data.localObservationsCount})`
                : '--';
        }
        if (histGoogleP5) {
            histGoogleP5.textContent = data.googleObservationsCount > 0
                ? `${(data.googleAvgPrecisionAt5 * 100).toFixed(1)}% (${data.googleObservationsCount})`
                : '--';
        }

        const observations = data.observations || [];
        if (!gcompHistoryTableBody) return;

        if (observations.length === 0) {
            gcompHistoryTableBody.innerHTML = '';
            if (gcompHistoryEmpty) gcompHistoryEmpty.style.display = 'block';
            return;
        }

        if (gcompHistoryEmpty) gcompHistoryEmpty.style.display = 'none';

        gcompHistoryTableBody.innerHTML = observations.map(obs => {
            const isLocal = obs.engine === 'local';
            const engineBadge = isLocal
                ? `<span class="gcomp-engine-badge local-badge">Local Engine (${escapeHtml(obs.localAlgo || 'coverage')})</span>`
                : `<span class="gcomp-engine-badge google-badge">Google Search</span>`;

            const precPct = (obs.precisionAt5 * 100).toFixed(1);
            const rtText = obs.responseTimeMs > 0 ? `${obs.responseTimeMs} ms` : '--';

            return `
                <tr>
                    <td style="font-size: 11px; color: var(--text-muted); font-family: var(--font-mono);">${escapeHtml(obs.formattedDate || '')}</td>
                    <td><strong>${escapeHtml(obs.query)}</strong></td>
                    <td>${engineBadge}</td>
                    <td style="font-weight: 700; color: #f1f5f9;">${obs.relevantCount} / 5</td>
                    <td>
                        <span style="font-weight: 700; color: ${obs.precisionAt5 >= 0.8 ? '#34d399' : (obs.precisionAt5 >= 0.6 ? '#60a5fa' : '#fbbf24')}; font-family: var(--font-mono);">
                            ${precPct}%
                        </span>
                    </td>
                    <td style="font-family: var(--font-mono); font-size: 12px;">${rtText}</td>
                    <td style="font-size: 12px; color: #cbd5e1; max-width: 250px;">${escapeHtml(obs.notes || '--')}</td>
                    <td style="text-align: right;">
                        <button type="button" class="btn-del-search" onclick="handleDeleteGoogleObservation('${escapeHtml(obs.id)}')" title="Delete Observation">
                            ✕
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        console.error('Failed to load Google comparison history:', err);
    }
}

async function handleDeleteGoogleObservation(id) {
    if (!id) return;
    try {
        const res = await fetch(`${API_BASE}/api/google-eval?action=delete&id=${encodeURIComponent(id)}`, {
            method: 'POST'
        });
        if (res.ok) {
            await loadGoogleComparisonHistory();
        }
    } catch (err) {
        console.error('Delete observation failed:', err);
    }
}

async function handleClearGoogleComparisonHistory() {
    if (!confirm('Are you sure you want to clear all saved Google comparison observations? This cannot be undone.')) {
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/api/google-eval?action=clear`, {
            method: 'POST'
        });
        if (res.ok) {
            await loadGoogleComparisonHistory();
        }
    } catch (err) {
        console.error('Clear comparison history failed:', err);
    }
}


