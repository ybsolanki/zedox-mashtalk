// ===== MOBILE MENU TOGGLE =====
const hamburger = document.getElementById('hamburger');
const navMenu = document.getElementById('navMenu');

if (hamburger) {
    hamburger.addEventListener('click', () => {
        navMenu.classList.toggle('active');
        hamburger.classList.toggle('active');
    });
}

// Close menu when clicking on a link
document.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', () => {
        navMenu.classList.remove('active');
        hamburger.classList.remove('active');
    });
});

// ===== SMOOTH SCROLL =====
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// ===== NAVBAR BACKGROUND ON SCROLL =====
window.addEventListener('scroll', () => {
    const navbar = document.querySelector('.navbar');
    if (window.scrollY > 100) {
        navbar.style.background = 'rgba(10, 10, 10, 0.98)';
        navbar.style.boxShadow = '0 5px 20px rgba(255, 107, 107, 0.1)';
    } else {
        navbar.style.background = 'rgba(10, 10, 10, 0.95)';
        navbar.style.boxShadow = 'none';
    }
});

// ===== ANIMATE ON SCROLL (Simple Implementation) =====
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('aos-animate');
        }
    });
}, observerOptions);

// Observe all elements with data-aos attribute
document.querySelectorAll('[data-aos]').forEach(element => {
    observer.observe(element);
});

// ===== DOWNLOAD BUTTON =====
const downloadBtn = document.getElementById('downloadBtn');
if (downloadBtn) {
    downloadBtn.addEventListener('click', () => {
        // Show coming soon message
        alert('🚀 MeshTalk is currently in development for Imagine Cup 2025!\n\n✅ Star our GitHub repo to get notified when we launch!\n\n📱 Expected Release: March 2025');
        
        // Optional: Track click (you can add analytics here)
        console.log('Download button clicked - redirecting to GitHub');
        
        // Redirect to GitHub releases page
        window.open('https://github.com/ybsolanki/zedox-mashtalk/releases', '_blank');
    });
}

// ===== TYPING EFFECT FOR HERO (Optional Cool Effect) =====
const heroDescription = document.querySelector('.hero-description');
if (heroDescription) {
    const text = heroDescription.textContent;
    heroDescription.textContent = '';
    let i = 0;
    
    function typeWriter() {
        if (i < text.length) {
            heroDescription.textContent += text.charAt(i);
            i++;
            setTimeout(typeWriter, 30);
        }
    }
    
    // Start typing effect after page loads
    setTimeout(typeWriter, 500);
}

// ===== STATS COUNTER ANIMATION =====
function animateCounter(element, target, duration = 2000) {
    let start = 0;
    const increment = target / (duration / 16);
    
    function updateCounter() {
        start += increment;
        if (start < target) {
            element.textContent = Math.floor(start);
            requestAnimationFrame(updateCounter);
        } else {
            element.textContent = target;
        }
    }
    
    updateCounter();
}

// Animate stats when they come into view
const statsObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting && !entry.target.dataset.animated) {
            const statNumber = entry.target.querySelector('.stat-number');
            if (statNumber) {
                const text = statNumber.textContent;
                // Only animate if it's a number
                if (text.includes('+')) {
                    const num = parseInt(text);
                    if (!isNaN(num)) {
                        statNumber.textContent = '0';
                        animateCounter(statNumber, num);
                        statNumber.textContent = statNumber.textContent + '+';
                    }
                }
                entry.target.dataset.animated = 'true';
            }
        }
    });
}, { threshold: 0.5 });

document.querySelectorAll('.stat').forEach(stat => {
    statsObserver.observe(stat);
});

// ===== PARTICLE EFFECT (Simple) =====
function createParticles() {
    const heroParticles = document.querySelector('.hero-particles');
    if (!heroParticles) return;
    
    for (let i = 0; i < 20; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        particle.style.cssText = `
            position: absolute;
            width: ${Math.random() * 4 + 2}px;
            height: ${Math.random() * 4 + 2}px;
            background: ${Math.random() > 0.5 ? '#FF6B6B' : '#4ECDC4'};
            border-radius: 50%;
            left: ${Math.random() * 100}%;
            top: ${Math.random() * 100}%;
            opacity: ${Math.random() * 0.5 + 0.2};
            animation: float ${Math.random() * 10 + 5}s ease-in-out infinite;
        `;
        heroParticles.appendChild(particle);
    }
}

// Create particles on load
window.addEventListener('load', createParticles);

// ===== FORM VALIDATION (if you add contact form later) =====
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

// ===== CONSOLE EASTER EGG =====
console.log('%c⚡ TEAM ZEDOX', 'color: #FF6B6B; font-size: 40px; font-weight: bold;');
console.log('%cMeshTalk - Communication Without Limits', 'color: #4ECDC4; font-size: 16px;');
console.log('%cImagine Cup 2025 | Built with ❤️', 'color: #999; font-size: 12px;');
console.log('%cInterested in our code? Check out: https://github.com/ybsolanki/zedox-mashtalk', 'color: #FF6B6B; font-size: 14px;');

// ===== PERFORMANCE MONITORING =====
window.addEventListener('load', () => {
    const loadTime = performance.now();
    console.log(`⚡ Page loaded in ${loadTime.toFixed(2)}ms`);
    
    // Log to analytics (add your analytics code here)
    if (typeof gtag !== 'undefined') {
        gtag('event', 'page_load_time', {
            'load_time': loadTime
        });
    }
});

// ===== DARK MODE TOGGLE (Future Enhancement) =====
// Uncomment when you want to add light mode option
/*
const themeToggle = document.getElementById('themeToggle');
if (themeToggle) {
    themeToggle.addEventListener('click', () => {
        document.body.classList.toggle('light-mode');
        localStorage.setItem('theme', document.body.classList.contains('light-mode') ? 'light' : 'dark');
    });
    
    // Load saved theme
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'light') {
        document.body.classList.add('light-mode');
    }
}
*/

// ===== SCROLL PROGRESS BAR =====
window.addEventListener('scroll', () => {
    const winScroll = document.body.scrollTop || document.documentElement.scrollTop;
    const height = document.documentElement.scrollHeight - document.documentElement.clientHeight;
    const scrolled = (winScroll / height) * 100;
    
    // If you add a progress bar element, update it here
    const progressBar = document.getElementById('scrollProgress');
    if (progressBar) {
        progressBar.style.width = scrolled + '%';
    }
});

// ===== COPY TO CLIPBOARD (for code snippets if needed) =====
function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        console.log('Copied to clipboard!');
    }).catch(err => {
        console.error('Failed to copy:', err);
    });
}

// ===== GITHUB STATS (Optional - fetch real data) =====
async function fetchGitHubStats() {
    try {
        const response = await fetch('https://api.github.com/repos/ybsolanki/zedox-mashtalk');
        const data = await response.json();
        
        // Update stats on page if elements exist
        const starsElement = document.getElementById('github-stars');
        const forksElement = document.getElementById('github-forks');
        
        if (starsElement) starsElement.textContent = data.stargazers_count;
        if (forksElement) forksElement.textContent = data.forks_count;
        
        console.log('⭐ GitHub Stars:', data.stargazers_count);
    } catch (error) {
        console.error('Error fetching GitHub stats:', error);
    }
}

// Fetch stats on load (optional)
// window.addEventListener('load', fetchGitHubStats);

// ===== INITIALIZE ALL =====
console.log('✅ MeshTalk website initialized');
