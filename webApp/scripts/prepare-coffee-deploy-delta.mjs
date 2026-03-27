import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";

async function ensureDir(dirPath) {
  await fs.mkdir(dirPath, { recursive: true });
}

async function readJson(filePath, fallback) {
  try {
    const raw = await fs.readFile(filePath, "utf8");
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

async function listCoffeeSlugs(coffeeRoot) {
  try {
    const entries = await fs.readdir(coffeeRoot, { withFileTypes: true });
    return entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name).sort();
  } catch {
    return [];
  }
}

async function sha256File(filePath) {
  const buffer = await fs.readFile(filePath);
  return crypto.createHash("sha256").update(buffer).digest("hex");
}

async function main() {
  const projectRoot = process.cwd();
  const distDir = path.resolve(projectRoot, "dist");
  const coffeeRoot = path.join(distDir, "coffee");
  const cacheDir = path.resolve(projectRoot, ".cache", "deploy-delta");
  const manifestPath = path.join(cacheDir, "coffee-pages-manifest.json");
  const uploadsPath = path.join(distDir, "coffee-uploads.txt");
  const deletesPath = path.join(distDir, "coffee-deletes.txt");

  await ensureDir(cacheDir);

  const previousManifest = await readJson(manifestPath, { version: 1, pages: {} });
  const previousPages = previousManifest?.version === 1 && previousManifest.pages ? previousManifest.pages : {};
  const nextPages = {};

  const slugs = await listCoffeeSlugs(coffeeRoot);
  const uploads = [];

  for (const slug of slugs) {
    const htmlPath = path.join(coffeeRoot, slug, "index.html");
    try {
      const hash = await sha256File(htmlPath);
      nextPages[slug] = { hash };
      if (previousPages[slug]?.hash !== hash) {
        uploads.push(`coffee/${slug}`);
      }
    } catch {
      // Si no se puede leer el HTML, no lo marcamos para deploy.
    }
  }

  const deletes = Object.keys(previousPages)
    .filter((slug) => !nextPages[slug])
    .map((slug) => `coffee/${slug}`)
    .sort();

  await fs.writeFile(uploadsPath, uploads.join("\n"), "utf8");
  await fs.writeFile(deletesPath, deletes.join("\n"), "utf8");
  await fs.writeFile(manifestPath, JSON.stringify({ version: 1, pages: nextPages }, null, 2), "utf8");

  console.log(
    `[prepare-coffee-deploy-delta] Coffee pages: total=${slugs.length}, upload=${uploads.length}, delete=${deletes.length}.`
  );
}

main().catch((error) => {
  console.error("[prepare-coffee-deploy-delta]", error);
  process.exitCode = 1;
});
