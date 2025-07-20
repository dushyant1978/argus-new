// Dashboard JavaScript functionality
document.addEventListener('DOMContentLoaded', function() {
    console.log('Argus Dashboard loaded');
});

function runFullScan() {
    const loadingModal = new bootstrap.Modal(document.getElementById('loadingModal'));
    loadingModal.show();
    
    fetch('/api/v1/scheduler/run', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        loadingModal.hide();
        
        if (data.status === 'success') {
            showAlert('success', 'Full scan completed successfully!');
            setTimeout(() => {
                window.location.reload();
            }, 2000);
        } else {
            showAlert('error', 'Scan failed: ' + (data.message || 'Unknown error'));
        }
    })
    .catch(error => {
        loadingModal.hide();
        console.error('Error running scan:', error);
        showAlert('error', 'Error running scan: ' + error.message);
    });
}

function runPageScan(pageName) {
    const button = event.target;
    const originalText = button.innerHTML;
    
    button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Running...';
    button.disabled = true;
    
    fetch(`/api/v1/scheduler/run/${encodeURIComponent(pageName)}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        button.innerHTML = originalText;
        button.disabled = false;
        
        if (data.status === 'success') {
            showAlert('success', `Scan completed for page: ${pageName}`);
            setTimeout(() => {
                window.location.reload();
            }, 2000);
        } else {
            showAlert('error', 'Scan failed: ' + (data.message || 'Unknown error'));
        }
    })
    .catch(error => {
        button.innerHTML = originalText;
        button.disabled = false;
        console.error('Error running page scan:', error);
        showAlert('error', 'Error running page scan: ' + error.message);
    });
}

function refreshDashboard() {
    window.location.reload();
}

function showAlert(type, message) {
    // Remove existing alerts
    const existingAlerts = document.querySelectorAll('.alert-dismissible');
    existingAlerts.forEach(alert => alert.remove());
    
    const alertClass = type === 'success' ? 'alert-success' : 'alert-danger';
    const iconClass = type === 'success' ? 'fa-check-circle' : 'fa-exclamation-triangle';
    
    const alertHTML = `
        <div class="alert ${alertClass} alert-dismissible fade show" role="alert">
            <i class="fas ${iconClass}"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    const container = document.querySelector('.container-fluid');
    const firstChild = container.firstElementChild;
    firstChild.insertAdjacentHTML('beforebegin', alertHTML);
    
    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        const alert = document.querySelector('.alert-dismissible');
        if (alert) {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }
    }, 5000);
}

function deletePage(pageId, pageName) {
    if (!confirm(`Are you sure you want to delete page "${pageName}"?`)) {
        return;
    }
    
    fetch(`/api/v1/scheduler/pages/${pageId}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            showAlert('success', `Page "${pageName}" deleted successfully`);
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            throw new Error('Failed to delete page');
        }
    })
    .catch(error => {
        console.error('Error deleting page:', error);
        showAlert('error', 'Error deleting page: ' + error.message);
    });
}

function togglePageStatus(pageId, pageName) {
    fetch(`/api/v1/scheduler/pages/${pageId}/toggle`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            showAlert('success', `Page "${pageName}" status updated`);
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            throw new Error(data.message || 'Unknown error');
        }
    })
    .catch(error => {
        console.error('Error toggling page status:', error);
        showAlert('error', 'Error updating page status: ' + error.message);
    });
}

function addNewPage() {
    const pageName = prompt('Enter page name:');
    if (!pageName) return;
    
    const cmsUrl = prompt('Enter CMS URL:');
    if (!cmsUrl) return;
    
    fetch('/api/v1/scheduler/pages', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            pageName: pageName,
            cmsUrl: cmsUrl
        })
    })
    .then(response => {
        if (response.ok) {
            showAlert('success', `Page "${pageName}" added successfully`);
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            throw new Error('Failed to add page');
        }
    })
    .catch(error => {
        console.error('Error adding page:', error);
        showAlert('error', 'Error adding page: ' + error.message);
    });
}

// Auto-refresh functionality for real-time updates
let autoRefreshInterval;
const AUTO_REFRESH_INTERVAL = 30000; // 30 seconds

function startAutoRefresh() {
    autoRefreshInterval = setInterval(() => {
        // Only refresh if no modals are open
        if (!document.querySelector('.modal.show')) {
            const currentTime = new Date().toLocaleTimeString();
            console.log(`Auto-refreshing dashboard at ${currentTime}`);
            
            // Refresh only the dynamic content, not the entire page
            refreshRecentReports();
            refreshHealthStatus();
        }
    }, AUTO_REFRESH_INTERVAL);
}

function stopAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
    }
}

function refreshRecentReports() {
    // This would typically make an AJAX call to get updated reports
    // For now, we'll just update the timestamp
    const timeElements = document.querySelectorAll('[data-timestamp]');
    timeElements.forEach(element => {
        // Update relative time display if needed
    });
}

function refreshHealthStatus() {
    // Check core service health
    fetch('/api/v1/anomaly/health')
    .then(response => response.json())
    .then(data => {
        const healthCard = document.querySelector('.health-status');
        if (healthCard) {
            const isHealthy = data.status === 'UP';
            healthCard.className = `card text-white ${isHealthy ? 'bg-success' : 'bg-danger'}`;
            healthCard.querySelector('h5').textContent = isHealthy ? 'Healthy' : 'Down';
            healthCard.querySelector('i').className = `fas fa-2x ${isHealthy ? 'fa-check-circle' : 'fa-times-circle'}`;
        }
    })
    .catch(error => {
        console.error('Error checking health status:', error);
    });
}

// Initialize auto-refresh when page loads
document.addEventListener('DOMContentLoaded', function() {
    startAutoRefresh();
    
    // Stop auto-refresh when page is hidden
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            stopAutoRefresh();
        } else {
            startAutoRefresh();
        }
    });
});

// Clean up when page unloads
window.addEventListener('beforeunload', function() {
    stopAutoRefresh();
});