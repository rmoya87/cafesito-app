#!/usr/bin/env node
/**
 * Genera un informe local de crashes a partir de .github/crash-reports/weekly.json.
 * No llama a ninguna API externa. La revisión y resolución se hace desde Cursor:
 * tú pides "revisa docs/crash-fixes/pending-review.md y resuelve" y la IA aplica
 * los cambios aquí; luego tú subes a Git cuando quieras.
 *
 * Formato del JSON:
 * [ { "title": "...", "stackTrace": "...", "file": "app/.../X.kt" (opcional), "count": N (opcional) }, ... ]
 */

import fs from 'fs';
import path from 'path';

const REPO_ROOT = process.env.GITHUB_WORKSPACE || process.cwd();
const CRASH_REPORT_PATH = path.join(REPO_ROOT, '.github', 'crash-reports', 'weekly.json');
const OUTPUT_DIR = path.join(REPO_ROOT, 'docs', 'crash-fixes');
const OUTPUT_FILE = path.join(OUTPUT_DIR, 'pending-review.md');

function loadCrashReport() {
  const fromEnv = process.env.CRASH_REPORT_JSON;
  if (fromEnv && fromEnv.trim()) {
    try {
      return JSON.parse(fromEnv);
    } catch (e) {
      console.error('CRASH_REPORT_JSON inválido:', e.message);
      return [];
    }
  }
  if (fs.existsSync(CRASH_REPORT_PATH)) {
    try {
      return JSON.parse(fs.readFileSync(CRASH_REPORT_PATH, 'utf8'));
    } catch (e) {
      console.error('Error leyendo weekly.json:', e.message);
      return [];
    }
  }
  return [];
}

function stackTraceToFilePath(stackTrace) {
  const fileMatch = stackTrace.match(/\((\w+\.kt):\d+\)/);
  const fileName = fileMatch ? fileMatch[1] : null;
  const classMatch = stackTrace.match(/at\s+([\w.]+)\.(\w+)\([^)]*\)/);
  if (!classMatch && !fileName) return null;
  const fullClass = classMatch ? classMatch[1] : '';
  let simpleName = fullClass ? fullClass.split('.').pop() : (fileName || '');
  if (fileName) simpleName = fileName;
  else if (simpleName && simpleName.endsWith('Kt')) simpleName = simpleName.slice(0, -2) + '.kt';
  else if (simpleName && !simpleName.endsWith('.kt')) simpleName = simpleName + '.kt';
  const packagePath = fullClass ? fullClass.replace(/\./g, '/').replace(/\/[^/]+$/, '') : '';
  const candidates = packagePath
    ? [
        path.join('app', 'src', 'main', 'java', packagePath, simpleName),
        path.join('shared', 'src', 'commonMain', 'kotlin', packagePath, simpleName),
      ]
    : [
        path.join('app', 'src', 'main', 'java', 'com', 'cafesito', 'app', simpleName),
        path.join('app', 'src', 'main', 'java', 'com', 'cafesito', 'app', 'ui', simpleName),
      ];
  for (const c of candidates) {
    const full = path.join(REPO_ROOT, c);
    if (fs.existsSync(full)) return c;
  }
  return path.join('app', 'src', 'main', 'java', packagePath.replace(/\/kotlin\/?/, ''), simpleName);
}

function getSourcePath(crash) {
  if (crash.file && fs.existsSync(path.join(REPO_ROOT, crash.file))) return crash.file;
  if (crash.stackTrace) return stackTraceToFilePath(crash.stackTrace);
  return null;
}

function main() {
  const crashes = loadCrashReport();
  if (!Array.isArray(crashes) || crashes.length === 0) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    fs.writeFileSync(
      OUTPUT_FILE,
      '# Revisión de crashes (pendiente)\n\nNo hay datos en `.github/crash-reports/weekly.json`.\n\nAñade un array de crashes con `title`, `stackTrace` y opcionalmente `file` y `count`. Ver `weekly.json.example`.\n'
    );
    console.log('Generado', OUTPUT_FILE, '(vacío). Añade datos en .github/crash-reports/weekly.json');
    return;
  }

  const lines = [
    '# Revisión de crashes (pendiente)',
    '',
    'Lista generada a partir de `.github/crash-reports/weekly.json`.',
    '**Desde Cursor:** pide "Revisa este informe y resuelve los crashes" para que la IA aplique los cambios aquí; luego tú subes a Git cuando quieras.',
    '',
    '---',
    '',
  ];

  crashes.forEach((crash, i) => {
    const filePath = getSourcePath(crash);
    lines.push(`## ${i + 1}. ${crash.title || 'Crash sin título'}`);
    if (crash.count) lines.push(`*Afectados: ${crash.count}*`);
    lines.push(`**Archivo:** \`${filePath || '—'}\``);
    if (filePath) lines.push(`**Ruta local:** \`${path.join(REPO_ROOT, filePath)}\``);
    lines.push('');
    lines.push('**Stack trace:**');
    lines.push('```');
    lines.push((crash.stackTrace || '(no indicado)').slice(0, 2000));
    lines.push('```');
    lines.push('');
    lines.push('---');
    lines.push('');
  });

  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  fs.writeFileSync(OUTPUT_FILE, lines.join('\n'));
  console.log('Generado', OUTPUT_FILE, 'con', crashes.length, 'crashes. Abre el archivo y pide en Cursor que los revise y resuelva.');
}

main();
