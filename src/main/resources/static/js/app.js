/**
 * SplitEase - Expense Sharing App
 * Google Pay Style JavaScript
 */

// ==================================
// Toast Notification System
// ==================================
const ToastManager = {
    container: null,

    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },

    show(type, title, message, duration = 4000) {
        this.init();

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const icons = {
            success: '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
            error: '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
            warning: '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
            info: '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
        };

        toast.innerHTML = `
            ${icons[type] || icons.info}
            <div class="toast-content">
                <div class="toast-title">${title}</div>
                ${message ? `<div class="toast-message">${message}</div>` : ''}
            </div>
            <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
        `;

        this.container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';
            setTimeout(() => toast.remove(), 300);
        }, duration);

        return toast;
    },

    success(title, message, duration) {
        return this.show('success', title, message, duration);
    },

    error(title, message, duration) {
        return this.show('error', title, message, duration);
    },

    warning(title, message, duration) {
        return this.show('warning', title, message, duration);
    },

    info(title, message, duration) {
        return this.show('info', title, message, duration);
    }
};

// ==================================
// Page Loader
// ==================================
const PageLoader = {
    element: null,

    init() {
        if (!this.element) {
            this.element = document.createElement('div');
            this.element.className = 'page-loader';
            this.element.innerHTML = `
                <div class="loader-content">
                    <div class="spinner spinner-lg"></div>
                    <div class="loader-text">Loading...</div>
                </div>
            `;
            document.body.appendChild(this.element);
        }
    },

    show(text = 'Loading...') {
        this.init();
        this.element.querySelector('.loader-text').textContent = text;
        this.element.classList.add('active');
    },

    hide() {
        if (this.element) {
            this.element.classList.remove('active');
        }
    }
};

// ==================================
// Button Loading State
// ==================================
function setButtonLoading(button, loading = true) {
    if (loading) {
        button.classList.add('loading');
        button.disabled = true;
        button.dataset.originalText = button.textContent;
    } else {
        button.classList.remove('loading');
        button.disabled = false;
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
        }
    }
}

// ==================================
// Form Submissions with Loader
// ==================================
document.addEventListener('DOMContentLoaded', function () {
    // Hide page loader when content is ready
    const pageLoader = document.getElementById('pageLoader');
    const progressLoader = document.getElementById('progressLoader');

    if (pageLoader) {
        setTimeout(() => {
            pageLoader.classList.add('hidden');
        }, 300);
    }

    if (progressLoader) {
        setTimeout(() => {
            progressLoader.style.display = 'none';
        }, 500);
    }

    // Auto-dismiss alerts after 5 seconds
    document.querySelectorAll('.alert').forEach(alert => {
        setTimeout(() => {
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 300);
        }, 5000);

        // Show toast for alerts
        const message = alert.textContent.trim();
        if (alert.classList.contains('alert-success')) {
            ToastManager.success('Success', message);
        } else if (alert.classList.contains('alert-error')) {
            ToastManager.error('Error', message);
        }
    });

    // Add loading state to forms
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', function (e) {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn && !submitBtn.classList.contains('btn-ghost')) {
                setButtonLoading(submitBtn, true);

                // Show progress loader during navigation
                if (progressLoader) {
                    progressLoader.style.display = 'block';
                }
            }
        });
    });

    // Split type tabs functionality
    initSplitTabs();

    // Modal functionality
    initModals();

    // Initialize expense form calculations
    initExpenseForm();
});

// ==================================
// Split Type Tabs
// ==================================
function initSplitTabs() {
    const tabs = document.querySelectorAll('.split-tab');
    const hiddenInput = document.getElementById('splitType');

    tabs.forEach(tab => {
        tab.addEventListener('click', function () {
            const type = this.dataset.type;

            // Update active tab
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            // Update hidden input
            if (hiddenInput) {
                hiddenInput.value = type;
            }

            // Show corresponding panel
            document.querySelectorAll('.split-panel').forEach(panel => {
                panel.classList.remove('active');
            });

            const targetPanel = document.getElementById('panel-' + type);
            if (targetPanel) {
                targetPanel.classList.add('active');
            }

            // Recalculate splits
            updateSplitCalculations();

            // Show notification
            ToastManager.info('Split Type Changed', `Now using ${getSplitTypeName(type)} split`);
        });
    });
}

function getSplitTypeName(type) {
    switch (type) {
        case 'EQUAL': return 'Equal';
        case 'EXACT': return 'Exact Amount';
        case 'PERCENTAGE': return 'Percentage';
        default: return type;
    }
}

// ==================================
// Expense Form Calculations
// ==================================
function initExpenseForm() {
    const amountInput = document.getElementById('expenseAmount');

    if (!amountInput) return;

    // Update calculations when amount changes
    amountInput.addEventListener('input', updateSplitCalculations);

    // Update when checkboxes change (equal split)
    document.querySelectorAll('.member-checkbox').forEach(cb => {
        cb.addEventListener('change', updateSplitCalculations);
    });

    // Update when exact amounts change
    document.querySelectorAll('.exact-amount-input').forEach(input => {
        input.addEventListener('input', updateExactTotal);
    });

    // Update when percentages change
    document.querySelectorAll('.percentage-input').forEach(input => {
        input.addEventListener('input', updatePercentageTotal);
    });

    // Initial calculation
    updateSplitCalculations();
}

function updateSplitCalculations() {
    const amount = parseFloat(document.getElementById('expenseAmount')?.value) || 0;

    // Equal split calculation
    const checkedMembers = document.querySelectorAll('.member-checkbox:checked');
    const numMembers = checkedMembers.length;
    const perPerson = numMembers > 0 ? (amount / numMembers) : 0;

    const equalPreview = document.getElementById('equal-split-preview');
    if (equalPreview) {
        equalPreview.textContent = '₹' + perPerson.toFixed(2);
    }

    // Update individual split amounts
    document.querySelectorAll('.split-amount').forEach(el => {
        const checkbox = el.closest('.member-row')?.querySelector('.member-checkbox');
        if (checkbox?.checked) {
            el.textContent = '₹' + perPerson.toFixed(2);
        } else {
            el.textContent = '₹0.00';
        }
    });

    // Update percentage calculations
    updatePercentageTotal();

    // Update exact total
    updateExactTotal();
}

function updateExactTotal() {
    const totalAmount = parseFloat(document.getElementById('expenseAmount')?.value) || 0;
    let sum = 0;

    document.querySelectorAll('.exact-amount-input').forEach(input => {
        sum += parseFloat(input.value) || 0;
    });

    const remaining = document.getElementById('exact-remaining');
    if (remaining) {
        remaining.textContent = '₹' + sum.toFixed(2) + ' / ₹' + totalAmount.toFixed(2);
        remaining.className = Math.abs(sum - totalAmount) < 0.01 ? 'text-success' : 'text-danger';
    }
}

function updatePercentageTotal() {
    const totalAmount = parseFloat(document.getElementById('expenseAmount')?.value) || 0;
    let totalPercent = 0;

    document.querySelectorAll('.percentage-input').forEach(input => {
        const percent = parseFloat(input.value) || 0;
        totalPercent += percent;

        // Update calculated amount
        const calcAmount = (totalAmount * percent / 100);
        const amountDisplay = input.closest('.member-row')?.querySelector('.calculated-amount');
        if (amountDisplay) {
            amountDisplay.textContent = '₹' + calcAmount.toFixed(2);
        }
    });

    const percentTotal = document.getElementById('percentage-total');
    if (percentTotal) {
        percentTotal.textContent = totalPercent.toFixed(1) + '%';
        percentTotal.className = Math.abs(totalPercent - 100) < 0.1 ? 'text-success' : 'text-danger';
    }
}

// ==================================
// Modal System
// ==================================
function initModals() {
    // Open modal buttons
    document.querySelectorAll('[data-modal]').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const modalId = this.dataset.modal;
            const modal = document.getElementById(modalId);
            if (modal) {
                modal.classList.add('active');
                ToastManager.info('Modal Opened', 'Fill in the details to continue');
            }
        });
    });

    // Close modal buttons
    document.querySelectorAll('.modal-close, [data-dismiss="modal"]').forEach(btn => {
        btn.addEventListener('click', function () {
            const modal = this.closest('.modal-overlay');
            if (modal) {
                modal.classList.remove('active');
            }
        });
    });

    // Close on overlay click
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', function (e) {
            if (e.target === this) {
                this.classList.remove('active');
            }
        });
    });

    // Close on Escape key
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-overlay.active').forEach(modal => {
                modal.classList.remove('active');
            });
        }
    });
}

// ==================================
// Form Validation
// ==================================
function validateForm(form) {
    let isValid = true;
    const requiredFields = form.querySelectorAll('[required]');

    requiredFields.forEach(field => {
        if (!field.value.trim()) {
            isValid = false;
            field.classList.add('error');
            field.style.borderColor = 'var(--danger)';

            field.addEventListener('input', function () {
                this.classList.remove('error');
                this.style.borderColor = '';
            }, { once: true });
        }
    });

    if (!isValid) {
        ToastManager.error('Validation Error', 'Please fill in all required fields');
    }

    return isValid;
}

// ==================================
// Delete Confirmation
// ==================================
function confirmDelete(message = 'Are you sure you want to delete this item?') {
    return new Promise((resolve) => {
        const confirmed = confirm(message);
        if (confirmed) {
            ToastManager.warning('Deleting', 'Removing item...');
        }
        resolve(confirmed);
    });
}

// ==================================
// Animated Counters
// ==================================
function animateValue(element, start, end, duration = 1000) {
    const range = end - start;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);

        // Easing function
        const easeOut = 1 - Math.pow(1 - progress, 3);
        const current = start + (range * easeOut);

        element.textContent = '₹' + current.toFixed(2);

        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }

    requestAnimationFrame(update);
}

// Animate balance amounts on page load
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.balance-amount').forEach(el => {
        const text = el.textContent;
        const match = text.match(/[₹$]?([\d,.]+)/);
        if (match) {
            const value = parseFloat(match[1].replace(/,/g, ''));
            if (!isNaN(value) && value > 0) {
                animateValue(el, 0, value, 800);
            }
        }
    });
});

// ==================================
// Member Quick Add
// ==================================
function addEmailToInput(email) {
    const input = document.getElementById('memberEmails');
    if (!input) return;

    const currentValue = input.value.trim();
    if (currentValue) {
        if (!currentValue.includes(email)) {
            input.value = currentValue + ', ' + email;
            ToastManager.success('Member Added', `${email} added to the list`);
        } else {
            ToastManager.warning('Already Added', `${email} is already in the list`);
        }
    } else {
        input.value = email;
        ToastManager.success('Member Added', `${email} added to the list`);
    }
}

// ==================================
// Navigation Active State
// ==================================
document.addEventListener('DOMContentLoaded', function () {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.navbar-nav a').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        } else if (currentPath.startsWith(link.getAttribute('href')) && link.getAttribute('href') !== '/dashboard') {
            link.classList.add('active');
        }
    });
});

// ==================================
// Utility Functions
// ==================================
function formatCurrency(amount) {
    return '₹' + parseFloat(amount).toFixed(2);
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Expose to global scope
window.ToastManager = ToastManager;
window.PageLoader = PageLoader;
window.setButtonLoading = setButtonLoading;
window.addEmailToInput = addEmailToInput;
window.confirmDelete = confirmDelete;
