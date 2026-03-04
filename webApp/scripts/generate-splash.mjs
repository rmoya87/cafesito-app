/**
 * Genera la imagen de splash PWA: logo centrado sobre fondo marrón (#6f4e37).
 * Ejecutar: npm run generate-splash
 * Salida: public/splash.png (y variantes por tamaño para iOS).
 */
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const PUBLIC = path.join(ROOT, "public");
const LOGO_PATH = path.join(PUBLIC, "logo.png");
const SPLASH_BG = "#6f4e37"; // caramel-accent, mismo marrón que el logo
const LOGO_MAX_WIDTH_RATIO = 0.4; // logo ocupa ~40% del ancho

const SIZES = [
  { w: 1284, h: 2778, name: "splash.png" },
  { w: 1170, h: 2532, name: "splash-1170x2532.png" },
  { w: 750, h: 1294, name: "splash-750x1294.png" },
];

async function main() {
  let sharp;
  try {
    sharp = (await import("sharp")).default;
  } catch {
    console.error("Instala sharp: npm install -D sharp");
    process.exit(1);
  }

  const logoBuf = await fs.readFile(LOGO_PATH);
  const logo = sharp(logoBuf);
  const logoMeta = await logo.metadata();
  const logoW = logoMeta.width ?? 512;
  const logoH = logoMeta.height ?? 512;

  for (const { w, h, name } of SIZES) {
    const maxLogoW = Math.round(w * LOGO_MAX_WIDTH_RATIO);
    const scale = maxLogoW / logoW;
    const scaledLogoW = Math.round(logoW * scale);
    const scaledLogoH = Math.round(logoH * scale);
    const x = Math.round((w - scaledLogoW) / 2);
    const y = Math.round((h - scaledLogoH) / 2);

    const resizedLogo = await logo
      .clone()
      .resize(scaledLogoW, scaledLogoH, { fit: "inside" })
      .toBuffer();

    const splash = await sharp({
      create: {
        width: w,
        height: h,
        channels: 3,
        background: SPLASH_BG,
      },
    })
      .composite([{ input: resizedLogo, top: y, left: x }])
      .png()
      .toBuffer();

    const outPath = path.join(PUBLIC, name);
    await fs.writeFile(outPath, splash);
    console.log("Escrito:", outPath);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
