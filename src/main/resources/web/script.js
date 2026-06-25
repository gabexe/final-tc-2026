document.addEventListener('DOMContentLoaded', () => {
    const editor = document.getElementById('code-editor');
    const highlighting = document.getElementById('highlighting');
    const highlightingContent = document.getElementById('highlighting-content');
    const timelineContainer = document.getElementById('timeline-container');
    const statusIndicator = document.getElementById('status-indicator');
    const btnCopy = document.getElementById('btn-copy');

    let debounceTimer;
    let lastCode = "";
    let currentRawOutput = "";

    btnCopy.addEventListener('click', () => {
        if (!currentRawOutput) return;
        navigator.clipboard.writeText(currentRawOutput).then(() => {
            const originalHTML = btnCopy.innerHTML;
            btnCopy.innerHTML = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--ansi-green)" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
            setTimeout(() => { btnCopy.innerHTML = originalHTML; }, 2000);
        });
    });

    function updateCode() {
        let code = editor.value;
        if (code[code.length - 1] === "\n") {
            code += " ";
        }
        highlightingContent.textContent = code;
        Prism.highlightElement(highlightingContent);
    }

    editor.addEventListener('input', () => {
        updateCode();
        
        const currentCode = editor.value.trim();
        if (currentCode === lastCode) return;
        lastCode = currentCode;

        clearTimeout(debounceTimer);
        if (currentCode) {
            debounceTimer = setTimeout(() => {
                compile();
            }, 500);
        }
    });

    editor.addEventListener('scroll', () => {
        highlighting.scrollTop = editor.scrollTop;
        highlighting.scrollLeft = editor.scrollLeft;
    });

    editor.addEventListener('keydown', function(e) {
        if (e.key === 'Tab') {
            e.preventDefault();
            const start = this.selectionStart;
            const end = this.selectionEnd;
            this.value = this.value.substring(0, start) + "    " + this.value.substring(end);
            this.selectionStart = this.selectionEnd = start + 4;
            updateCode();
            
            const currentCode = editor.value.trim();
            if (currentCode === lastCode) return;
            lastCode = currentCode;

            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(compile, 500);
        }
    });

    updateCode();
    const initialCode = editor.value.trim();
    if (initialCode) {
        lastCode = initialCode;
        debounceTimer = setTimeout(compile, 500);
    }

    function setStatus(status, text) {
        statusIndicator.className = `status-indicator ${status}`;
        statusIndicator.innerHTML = `<div class="dot"></div>${text}`;
    }

    async function compile() {
        const code = editor.value.trim();
        if (!code) return;

        setStatus('compiling', 'Compilando...');
        timelineContainer.innerHTML = '<div class="timeline" id="timeline"></div>';

        try {
            const response = await fetch('/api/compile', {
                method: 'POST',
                headers: { 'Content-Type': 'text/plain' },
                body: code
            });
            const data = await response.json();
            
            if (data.output) {
                currentRawOutput = stripAnsi(data.output);
                btnCopy.style.display = 'flex';
                await simulateEvolution(data.output);
                if (statusIndicator.classList.contains('compiling')) {
                    setStatus('success', 'Completado');
                }
            } else if (data.error) {
                currentRawOutput = data.error;
                btnCopy.style.display = 'flex';
                addNode("Error del servidor", data.error, "error");
                setStatus('error', 'Error');
            }
        } catch (err) {
            addNode("Error de Red", err.message, "error");
            setStatus('error', 'Fallo de Red');
        }
    }

    const scrollObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            } else {
                entry.target.classList.remove('visible');
            }
        });
    }, {
        root: timelineContainer,
        threshold: 0.15,
        rootMargin: '0px 0px -20px 0px'
    });

    async function simulateEvolution(rawOutput) {
        const lines = rawOutput.split('\n');
        let currentSectionTitle = "Iniciando";
        let currentSectionContent = [];
        
        const sections = [];
        
        for (let line of lines) {
            let strippedLine = stripAnsi(line);
            if (strippedLine.startsWith('---') && strippedLine.endsWith('---')) {
                if (currentSectionContent.length > 0) {
                    sections.push({ title: currentSectionTitle, content: currentSectionContent });
                }
                currentSectionTitle = strippedLine.replace(/---/g, '').trim();
                currentSectionContent = [];
            } else {
                if (strippedLine.trim() !== '' || currentSectionContent.length > 0) {
                    currentSectionContent.push(line);
                }
            }
        }
        if (currentSectionContent.length > 0) {
            sections.push({ title: currentSectionTitle, content: currentSectionContent });
        }

        const timeline = document.getElementById('timeline');
        
        addNode("Análisis Léxico y Sintáctico", "Escaneando tokens y verificando la gramática del código fuente...", "normal");

        for (const section of sections) {
            if (section.title.includes("Iniciando")) continue;
            
            let status = "normal";
            let lowerTitle = section.title.toLowerCase();
            
            let rawContentStr = stripAnsi(section.content.join('\n')).trim();
            if (!rawContentStr) continue;
            
            let lowerContent = rawContentStr.toLowerCase();

            if (lowerTitle.includes("error") || lowerContent.includes("✗") || lowerContent.includes("error")) status = "error";
            if (lowerContent.includes("✓") || lowerContent.includes("exitosa")) status = "success";

            let htmlContent = '';
            if (lowerTitle.includes("ast") || lowerTitle.includes("árbol")) {
                htmlContent = formatAST(section.content);
            } else {
                htmlContent = formatANSI(section.content.join('\n'));
            }

            addNode(section.title, htmlContent, status);
            
            if (rawContentStr.includes("Compilación fallida")) {
                status = "error";
                setStatus('error', 'Fallido');
            }
        }

        timelineContainer.scrollTop = 0;
    }

    function addNode(title, contentHTML, statusClass) {
        const timeline = document.getElementById('timeline');
        const node = document.createElement('div');
        node.className = `timeline-node ${statusClass}`;
        
        node.innerHTML = `
            <div class="timeline-dot"></div>
            <div class="node-title">${escapeHTML(title)}</div>
            <div class="node-content">${contentHTML}</div>
        `;
        timeline.appendChild(node);
        
        scrollObserver.observe(node);
    }

    function stripAnsi(str) {
        return str.replace(/[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]/g, '');
    }

    function escapeHTML(str) {
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function formatANSI(str) {
        let escaped = escapeHTML(str);
        return escaped
            .replace(/\u001B\[31m/g, '<span style="color: var(--ansi-red); font-weight: 600;">') 
            .replace(/\u001B\[32m/g, '<span style="color: var(--ansi-green); font-weight: 600;">') 
            .replace(/\u001B\[33m/g, '<span style="color: var(--ansi-yellow); font-weight: 600;">') 
            .replace(/\u001B\[0m/g, '</span>');
    }

    function formatAST(lines) {
        let html = '';
        for (let line of lines) {
            let strippedLine = stripAnsi(line);
            
            const match = strippedLine.match(/^(\s*)/);
            const spaces = match ? match[1].length : 0;
            const indentSize = spaces * 8; // Pixels
            
            const htmlText = formatANSI(line.trim());
            
            if (strippedLine.trim().length > 0) {
                html += `<div class="ast-line" style="--indent: ${indentSize}px;">${htmlText}</div>`;
            }
        }
        return html;
    }
});
