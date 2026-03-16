/**
 * Copia los .woff2 de @fontsource-variable/material-symbols-outlined a public/fonts
 * para que la app los sirva como estáticos y evite OTS/fallos al cargar desde node_modules en dev.
 * Se ejecuta en postinstall.
 */
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const PKG = path.join(ROOT, "node_modules", "@fontsource-variable", "material-symbols-outlined", "files");
const OUT_DIR = path.join(ROOT, "public", "fonts");

async function main() {
  try {
    await fs.mkdir(OUT_DIR, { recursive: true });
  } catch (e) {
    if (e.code !== "EEXIST") throw e;
  }
  let files;
  try {
    files = await fs.readdir(PKG);
  } catch (e) {
    if (e.code === "ENOENT") {
      console.warn("[copy-material-symbols-font] node_modules/@fontsource-variable/material-symbols-outlined/files no encontrado; ejecuta npm install.");
      return;
    }
    throw e;
  }
  const woff2 = files.filter((f) => f.endsWith(".woff2"));
  for (const name of woff2) {
    const src = path.join(PKG, name);
    const dest = path.join(OUT_DIR, name);
    await fs.copyFile(src, dest);
    console.log("[copy-material-symbols-font] copiado:", name);
  }
  if (woff2.length === 0) console.warn("[copy-material-symbols-font] No se encontraron .woff2 en", PKG);
}

main().catch((err) => {
  console.error("[copy-material-symbols-font]", err);
  process.exit(1);
});
