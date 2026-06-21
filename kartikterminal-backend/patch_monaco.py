import re
import sys

with open("src/main/resources/static/compiler.html", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Remove Prism CSS
content = content.replace('<link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css" rel="stylesheet">\n', '')

# 2. Replace editor HTML
editor_html = """    <div class="editor-wrapper">
      <div class="line-numbers" id="lineNumbers"><div>1</div></div>
      <div class="editor-area">
        <div class="line-highlight" id="lineHighlight"></div>
        <pre id="highlight-layer" aria-hidden="true"><code id="highlightCode"></code></pre>
        <textarea id="code-editor" spellcheck="false" onscroll="syncScroll()" oninput="handleInput()" onpaste="handlePaste()" onkeydown="handleKeydown(event)" onclick="updateLineHighlight()" onkeyup="updateLineHighlight()"></textarea>
      </div>
    </div>"""
new_editor_html = '    <div class="editor-wrapper" id="monaco-container" style="width: 100%; height: 100%;"></div>'
content = content.replace(editor_html, new_editor_html)

# 3. Replace Prism Scripts with Monaco
prism_scripts = """<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-python.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-cpp.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-c.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-go.min.js"></script>
<script>"""
monaco_scripts = """<script src="https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs/loader.js"></script>
<script>"""
content = content.replace(prism_scripts, monaco_scripts)

# 4. Replace DOM elements
dom_elements = """const editor       = document.getElementById('code-editor');
const highlightCode= document.getElementById('highlightCode');
const highlightLayer=document.getElementById('highlight-layer');
const lineNumbers  = document.getElementById('lineNumbers');"""
new_dom = """const monacoContainer = document.getElementById('monaco-container');
const editor = {
  get value() { return window.monacoEditor ? window.monacoEditor.getValue() : ""; },
  set value(v) { if (window.monacoEditor) window.monacoEditor.setValue(v); },
  selectionStart: 0, selectionEnd: 0, scrollTop: 0, scrollLeft: 0
};"""
content = content.replace(dom_elements, new_dom)

# 5. Replace handleInput and handlePaste
old_input = """function handleInput() {
  let code=editor.value; if(code.endsWith('\\n')) code+=' ';
  const lang=document.getElementById('language').value;
  const prismLang=Prism.languages[lang==='js'?'javascript':lang]||Prism.languages.clike;
  highlightCode.innerHTML=Prism.highlight(code,prismLang,lang);
  renderLineNumbers(); syncScroll(); updateStats(); updateLineHighlight(); saveToLocalStorage(); updateDetailedStats();
  if(searchQuery) performSearch(searchQuery);
}
function handlePaste(e) {
  setTimeout(()=>{
    handleInput();
    if(securityMode){
      playSecurityAlert(); logToTerminal("⚠️ Security: Code Paste Detected","warning");
      violations++;
      if(violations>=MAX_VIOLATIONS){isBlocked=true;document.body.classList.add('locked');document.getElementById('runBtn').disabled=true;openModal("Maximum paste violations reached. Editor Locked.");}
    } else { logToTerminal("📋 Code pasted","info"); }
  },10);
}"""
new_input = """function handleInput() {
  updateStats(); updateDetailedStats(); saveToLocalStorage();
}
function handlePaste() {
  if(securityMode){
    playSecurityAlert(); logToTerminal("⚠️ Security: Code Paste Detected","warning");
    violations++;
    if(violations>=MAX_VIOLATIONS){isBlocked=true;document.body.classList.add('locked');document.getElementById('runBtn').disabled=true;openModal("Maximum paste violations reached. Editor Locked.");}
  } else { logToTerminal("📋 Code pasted","info"); }
}"""
content = content.replace(old_input, new_input)

# 6. Replace unused Prism functions
old_funcs = """function renderLineNumbers() { const c=editor.value.split('\\n').length; lineNumbers.innerHTML=Array.from({length:c},(_,i)=>`<div>${i+1}</div>`).join(''); }
function syncScroll() { highlightLayer.scrollTop=editor.scrollTop; highlightLayer.scrollLeft=editor.scrollLeft; lineNumbers.scrollTop=editor.scrollTop; if(autoScroll) terminal.scrollTop=terminal.scrollHeight; updateLineHighlight(); }
function updateLineHighlight() { const lh=1.6*fontSize, pt=20, st=editor.scrollTop, tbc=editor.value.substring(0,editor.selectionStart), cl=tbc.split('\\n').length, top=pt+(cl-1)*lh-st; lineHighlight.style.top=`${top}px`; lineHighlight.style.height=`${lh}px`; }"""
new_funcs = """function renderLineNumbers() {}
function syncScroll() {}
function updateLineHighlight() {}"""
content = content.replace(old_funcs, new_funcs)

# 7. Replace formatCode
old_format = """function formatCode() {
  let code=editor.value; const lines=code.split('\\n'); let indent=0;
  const formatted=lines.map(line=>{ const t=line.trim(); if(t.endsWith('{'))indent++; else if(t.startsWith('}'))indent=Math.max(0,indent-1); return '    '.repeat(Math.max(0,indent))+t; });
  editor.value=formatted.join('\\n'); handleInput(); saveToLocalStorage(); logToTerminal("✨ Code formatted","info");
}"""
new_format = """function formatCode() {
  if (window.monacoEditor) {
    window.monacoEditor.getAction('editor.action.formatDocument').run();
    logToTerminal("✨ Code formatted","info");
  }
}"""
content = content.replace(old_format, new_format)

# 8. Replace changeLanguage
old_lang = """function changeLanguage(showMessage=true) {
  const lang=document.getElementById('language').value;
  const fileName=`Main.${langExt[lang]}`;
  terminal.innerHTML=''; editor.value=defaultTemplates[lang]||'';
  document.getElementById('file-name').innerText=fileName;
  document.getElementById('current-path').innerText=fileName;
  handleInput(); saveToLocalStorage();
  if(showMessage) logToTerminal(`🔄 Language changed to ${lang.toUpperCase()}`,"warning");
}"""
new_lang = """function changeLanguage(showMessage=true) {
  const lang=document.getElementById('language').value;
  const fileName=`Main.${langExt[lang]}`;
  terminal.innerHTML=''; 
  if (window.monacoEditor) {
    const model = window.monacoEditor.getModel();
    monaco.editor.setModelLanguage(model, lang === 'js' ? 'javascript' : lang);
    editor.value = defaultTemplates[lang]||'';
  } else { editor.value=defaultTemplates[lang]||''; }
  document.getElementById('file-name').innerText=fileName;
  document.getElementById('current-path').innerText=fileName;
  handleInput(); saveToLocalStorage();
  if(showMessage) logToTerminal(`🔄 Language changed to ${lang.toUpperCase()}`,"warning");
}"""
content = content.replace(old_lang, new_lang)

# 9. Append Monaco Initialization at the end
monaco_init = """
require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs' }});
require(['vs/editor/editor.main'], function() {
    window.monacoEditor = monaco.editor.create(document.getElementById('monaco-container'), {
        value: defaultTemplates['java'],
        language: 'java',
        theme: 'vs-dark',
        automaticLayout: true,
        fontSize: 14,
        minimap: { enabled: false }
    });
    window.monacoEditor.onDidChangeModelContent(() => { handleInput(); });
    window.monacoEditor.onDidPaste(() => { handlePaste(); });
});
"""
content = content.replace("</script>\n</body>", monaco_init + "</script>\n</body>")

with open("src/main/resources/static/compiler.html", "w", encoding="utf-8") as f:
    f.write(content)

print("compiler.html updated with Monaco Editor.")
