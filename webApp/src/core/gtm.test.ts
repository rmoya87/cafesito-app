/**
 * Tests del dataLayer: comprueban que los payloads coinciden con la configuración GTM Web
 * (variables DLV, disparadores CE y etiquetas GA4 según docs/gtm/CONTAINER_REFERENCE_WEB.md).
 */
import { describe, it, expect, beforeEach, vi } from "vitest";
import {
  pushPageView,
  pushUserId,
  pushEvent,
  getGtmContainerId,
  isGtmEnabled
} from "./gtm";

describe("GTM dataLayer", () => {
  let dataLayer: unknown[];

  beforeEach(() => {
    dataLayer = [];
    if (typeof window !== "undefined") {
      (window as unknown as { dataLayer: unknown[] }).dataLayer = dataLayer;
    }
  });

  describe("pushPageView", () => {
    it("empuja event page_view con page_path, page_location, screen_name (claves que lee GTM)", () => {
      pushPageView("/home");
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("page_view");
      expect(last).toHaveProperty("page_path", "/home");
      expect(last).toHaveProperty("page_location");
      expect(last).toHaveProperty("screen_name", "home");
    });

    it("incluye page_title cuando se pasa (DLV - page_title en GTM)", () => {
      pushPageView("/coffee/achicoria/", "Café · Achicoria");
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("page_view");
      expect(last.page_title).toBe("Café · Achicoria");
      expect(last.screen_name).toBe("detail");
      expect(last.page_path).toBe("/coffee/achicoria/");
    });

    it("normaliza screen_name para detalle café (paridad Android)", () => {
      pushPageView("/coffee/cualquier-slug/");
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.screen_name).toBe("detail");
    });

    it("normaliza screen_name para profile y secciones", () => {
      pushPageView("/profile/user123/favorites");
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.screen_name).toBe("profile/favorites");
    });
  });

  describe("pushUserId", () => {
    it("empuja event set_user_id con user_id (CE - set_user_id, DLV - user_id)", () => {
      pushUserId("42");
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("set_user_id");
      expect(last.user_id).toBe("42");
    });

    it("empuja user_id vacío cuando userId es null (logout, DLV - user_id en GTM)", () => {
      pushUserId(null);
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("set_user_id");
      expect(last.user_id).toBe("");
    });
  });

  describe("pushEvent", () => {
    it("empuja modal_open con modal_id (CE - modal_open, DLV - modal_id)", () => {
      pushEvent("modal_open", { modal_id: "login_sheet" });
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("modal_open");
      expect(last.modal_id).toBe("login_sheet");
    });

    it("empuja modal_close con modal_id (CE - modal_close)", () => {
      pushEvent("modal_close", { modal_id: "list_edit" });
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("modal_close");
      expect(last.modal_id).toBe("list_edit");
    });

    it("empuja button_click con button_id (CE - button_click, DLV - button_id)", () => {
      pushEvent("button_click", { button_id: "brew_save_to_diary" });
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("button_click");
      expect(last.button_id).toBe("brew_save_to_diary");
    });

    it("empuja carousel_nav con carousel_id y direction (CE - carousel_nav)", () => {
      pushEvent("carousel_nav", { carousel_id: "brew_despensa", direction: "next" });
      const last = dataLayer[dataLayer.length - 1] as Record<string, unknown>;
      expect(last.event).toBe("carousel_nav");
      expect(last.carousel_id).toBe("brew_despensa");
      expect(last.direction).toBe("next");
    });
  });

  describe("getGtmContainerId / isGtmEnabled", () => {
    it("isGtmEnabled y getGtmContainerId dependen de env en build", () => {
      expect(typeof getGtmContainerId()).toBe("string");
      expect(typeof isGtmEnabled()).toBe("boolean");
    });
  });
});
