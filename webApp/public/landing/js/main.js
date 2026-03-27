// Theme Management
const themeToggleBtn = document.getElementById('theme-toggle');
const root = document.documentElement;

function setTheme(theme) {
    root.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
    updateToggleIcon(theme);
}

function updateToggleIcon(theme) {
    // Simple text or SVG swap could go here
    if (themeToggleBtn) {
        themeToggleBtn.innerHTML = theme === 'dark'
            ? '<svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg>' // Sun icon
            : '<svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" /></svg>'; // Moon icon
    }
}

function initTheme() {
    const savedTheme = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (savedTheme) {
        setTheme(savedTheme);
    } else {
        setTheme(prefersDark ? 'dark' : 'light');
    }
}

if (themeToggleBtn) {
    themeToggleBtn.addEventListener('click', () => {
        const currentTheme = root.getAttribute('data-theme');
        setTheme(currentTheme === 'dark' ? 'light' : 'dark');
    });
}

// Cookie Consent
const cookieBanner = document.getElementById('cookie-banner');
const acceptCookiesBtn = document.getElementById('accept-cookies');
const rejectCookiesBtn = document.getElementById('reject-cookies');
const GA_ID = 'G-XXXXXXXXXX'; // Placeholder

function loadAnalytics() {
    console.log('Loading Analytics:', GA_ID);
    // Implementation of GA4 injection would go here
    /*
    const script = document.createElement('script');
    script.src = `https://www.googletagmanager.com/gtag/js?id=${GA_ID}`;
    script.async = true;
    document.head.appendChild(script);
  
    window.dataLayer = window.dataLayer || [];
    function gtag(){dataLayer.push(arguments);}
    gtag('js', new Date());
    gtag('config', GA_ID);
    */
}

function initCookies() {
    const consent = localStorage.getItem('cookie-consent');

    if (!consent && cookieBanner) {
        cookieBanner.classList.add('visible');
    } else if (consent === 'accepted') {
        loadAnalytics();
    }
}

if (acceptCookiesBtn) {
    acceptCookiesBtn.addEventListener('click', () => {
        localStorage.setItem('cookie-consent', 'accepted');
        cookieBanner.classList.remove('visible');
        loadAnalytics();
    });
}

if (rejectCookiesBtn) {
    rejectCookiesBtn.addEventListener('click', () => {
        localStorage.setItem('cookie-consent', 'rejected');
        cookieBanner.classList.remove('visible');
    });
}

// Mobile Menu
const menuToggle = document.getElementById('menu-toggle');
const mobileMenu = document.getElementById('mobile-menu');

if (menuToggle && mobileMenu) {
    menuToggle.addEventListener('click', () => {
        const isExpanded = menuToggle.getAttribute('aria-expanded') === 'true';
        menuToggle.setAttribute('aria-expanded', !isExpanded);
        mobileMenu.classList.toggle('hidden');
        mobileMenu.classList.toggle('flex');
    });
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initCookies();

    // FAQ Accordion
    const accordionHeaders = document.querySelectorAll('.accordion-header');
    accordionHeaders.forEach(header => {
        header.addEventListener('click', () => {
            const isExpanded = header.getAttribute('aria-expanded') === 'true';
            const body = header.nextElementSibling;

            // Toggle current
            header.setAttribute('aria-expanded', !isExpanded);
            body.classList.toggle('open');
            body.style.maxHeight = !isExpanded ? body.scrollHeight + "px" : null;

            // Optional: Close others
            /*
            accordionHeaders.forEach(otherHeader => {
                if (otherHeader !== header) {
                    otherHeader.setAttribute('aria-expanded', 'false');
                    otherHeader.nextElementSibling.classList.remove('open');
                    otherHeader.nextElementSibling.style.maxHeight = null;
                }
            });
            */
        });
    });

    // Scroll Animations
    const observerOptions = {
        root: null,
        rootMargin: '0px',
        threshold: 0.15
    };

    const observer = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
                observer.unobserve(entry.target); // Trigger once
            }
        });
    }, observerOptions);

    document.querySelectorAll('.reveal-on-scroll, .reveal-stagger').forEach((el) => {
        observer.observe(el);
    });

    // Header Scroll Effect
    const header = document.querySelector('.header');
    window.addEventListener('scroll', () => {
        if (window.scrollY > 20) {
            header.classList.add('scrolled');
        } else {
            header.classList.remove('scrolled');
        }
    });

    // Mobile Menu Toggle
    window.toggleMobileMenu = function () {
        const menu = document.querySelector('.mobile-menu');
        const btn = document.querySelector('.hamburger-btn');
        const isOpen = !menu.classList.contains('open');
        menu.classList.toggle('open');
        btn.classList.toggle('open');
        if (btn) btn.setAttribute('aria-expanded', isOpen);

        // Prevent scrolling when menu is open
        if (menu.classList.contains('open')) {
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = '';
        }
    }

    const closeMobileMenu = () => {
        const menu = document.querySelector('.mobile-menu');
        const btn = document.querySelector('.hamburger-btn');
        if (!menu || !btn) return;
        menu.classList.remove('open');
        btn.classList.remove('open');
        btn.setAttribute('aria-expanded', 'false');
        document.body.style.overflow = '';
    };

    document.querySelectorAll('.mobile-menu a').forEach((link) => {
        link.addEventListener('click', () => {
            closeMobileMenu();
        });
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeMobileMenu();
        }
    });

    // Number Animation
    const statsObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const counters = entry.target.querySelectorAll('.stat-number');
                counters.forEach(counter => {
                    const targetStr = counter.getAttribute('data-target') || '';
                    const target = +targetStr.replace('+', '');
                    const suffix = counter.getAttribute('data-suffix') || '';
                    const addPlus = targetStr.startsWith('+');
                    const speed = 200;

                    const updateCount = () => {
                        const count = +counter.innerText.replace('+', '').replace(/\s*hrs\s*$/, '').replace('%', '');
                        const inc = target / speed;

                        if (count < target) {
                            counter.innerText = Math.ceil(count + inc);
                            if (counter.getAttribute('data-target') === '100') counter.innerText += '%';
                            else if (addPlus) counter.innerText = '+' + counter.innerText;
                            else if (suffix) counter.innerText += suffix;

                            setTimeout(updateCount, 20);
                        } else {
                            counter.innerText = target;
                            if (counter.getAttribute('data-target') === '100') counter.innerText += '%';
                            else if (addPlus) counter.innerText = '+' + target;
                            else if (suffix) counter.innerText = target + suffix;
                        }
                    };
                    updateCount();
                });
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.5 });

    const statsSection = document.querySelector('.stats-grid');
    if (statsSection) {
        statsObserver.observe(statsSection);
    }

    // Testimonial Carousel Indicators
    const testimonialGrid = document.querySelector('.testimonial-grid');
    const indicators = document.querySelectorAll('.indicator-dot');

    if (testimonialGrid && indicators.length > 0) {
        // Update active indicator based on scroll position
        testimonialGrid.addEventListener('scroll', () => {
            const scrollLeft = testimonialGrid.scrollLeft;
            const cardWidth = testimonialGrid.querySelector('.testimonial-card').offsetWidth;
            const gap = 16; // Match the gap in CSS
            const activeIndex = Math.round(scrollLeft / (cardWidth + gap));

            indicators.forEach((dot, index) => {
                if (index === activeIndex) {
                    dot.classList.add('active');
                } else {
                    dot.classList.remove('active');
                }
            });
        });

        // Click indicator to scroll to that card
        indicators.forEach((dot, index) => {
            dot.addEventListener('click', () => {
                const cardWidth = testimonialGrid.querySelector('.testimonial-card').offsetWidth;
                const gap = 16;
                const scrollPosition = index * (cardWidth + gap);
                testimonialGrid.scrollTo({
                    left: scrollPosition,
                    behavior: 'smooth'
                });
            });
        });
    }
});
