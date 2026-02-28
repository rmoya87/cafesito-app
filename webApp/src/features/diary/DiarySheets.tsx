import type { ReactNode } from "react";
import { useState, useMemo, useEffect } from "react";
import type { CoffeeRow, PantryItemRow } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { Button, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";

type PantryCoffeeRow = { item: PantryItemRow; coffee: CoffeeRow };
type CoffeeSheetStep = "select" | "dose" | "tipo" | "tamaño" | "createCoffee";

function DiarySheets({
  isActive,
  showQuickActions,
  showPeriodSheet,
  showWaterSheet,
  showCoffeeSheet,
  showAddPantrySheet,
  onCloseQuickActions,
  onClosePeriodSheet,
  onCloseWaterSheet,
  onCloseCoffeeSheet,
  onCloseAddPantrySheet,
  onOpenWaterSheet,
  onOpenCoffeeSheet,
  onOpenAddPantrySheet,
  diaryPeriod,
  setDiaryPeriod,
  diaryWaterMlDraft,
  setDiaryWaterMlDraft,
  onSaveWater,
  pantryCoffeeRows,
  activeUserId,
  diaryCoffeeOptions,
  coffeeSheetStep,
  setCoffeeSheetStep,
  diaryCoffeeIdDraft,
  setDiaryCoffeeIdDraft,
  diaryCoffeeGramsDraft,
  setDiaryCoffeeGramsDraft,
  diaryCoffeePreparationDraft,
  setDiaryCoffeePreparationDraft,
  diaryCoffeeMlDraft,
  setDiaryCoffeeMlDraft,
  diaryCoffeeCaffeineDraft,
  setDiaryCoffeeCaffeineDraft,
  onSaveCoffee,
  lastCreatedCoffeeNameForSheet = null,
  pantrySheetStep,
  setPantrySheetStep,
  diaryPantryCoffeeIdDraft,
  setDiaryPantryCoffeeIdDraft,
  diaryPantryGramsDraft,
  setDiaryPantryGramsDraft,
  bumpPantryGrams,
  onSavePantry,
  selectedDiaryPantryCoffee,
  createCoffeeFormContent,
  onCreateCoffeeNext,
  createCoffeeFormForPantrySheet,
  onCreateCoffeeNextForPantry,
  isCreateCoffeeFormValid = false,
  showBarcodeButton,
  onBarcodeClick,
  barcodeDetectedValue,
  onClearBarcodeDetectedValue
}: {
  isActive: boolean;
  showQuickActions: boolean;
  showPeriodSheet: boolean;
  showWaterSheet: boolean;
  showCoffeeSheet: boolean;
  showAddPantrySheet: boolean;
  onCloseQuickActions: () => void;
  onClosePeriodSheet: () => void;
  onCloseWaterSheet: () => void;
  onCloseCoffeeSheet: () => void;
  onCloseAddPantrySheet: () => void;
  onOpenWaterSheet: () => void;
  onOpenCoffeeSheet: () => void;
  onOpenAddPantrySheet: () => void;
  diaryPeriod: "hoy" | "7d" | "30d";
  setDiaryPeriod: (value: "hoy" | "7d" | "30d") => void;
  diaryWaterMlDraft: string;
  setDiaryWaterMlDraft: (value: string) => void;
  onSaveWater: () => Promise<void>;
  pantryCoffeeRows: PantryCoffeeRow[];
  activeUserId: number | null;
  diaryCoffeeOptions: CoffeeRow[];
  coffeeSheetStep: CoffeeSheetStep;
  setCoffeeSheetStep: (value: CoffeeSheetStep) => void;
  diaryCoffeeIdDraft: string;
  setDiaryCoffeeIdDraft: (value: string) => void;
  diaryCoffeeGramsDraft: string;
  setDiaryCoffeeGramsDraft: (value: string) => void;
  diaryCoffeePreparationDraft: string;
  setDiaryCoffeePreparationDraft: (value: string) => void;
  diaryCoffeeMlDraft: string;
  setDiaryCoffeeMlDraft: (value: string) => void;
  diaryCoffeeCaffeineDraft: string;
  setDiaryCoffeeCaffeineDraft: (value: string) => void;
  onSaveCoffee: (payload?: {
    coffeeId: string | null;
    coffeeName: string;
    coffeeBrand?: string;
    amountMl: number;
    caffeineMg: number;
    coffeeGrams?: number;
    preparationType: string;
    sizeLabel?: string | null;
  }) => Promise<void>;
  lastCreatedCoffeeNameForSheet?: string | null;
  pantrySheetStep: "select" | "form" | "createCoffee";
  setPantrySheetStep: (value: "select" | "form" | "createCoffee") => void;
  diaryPantryCoffeeIdDraft: string;
  setDiaryPantryCoffeeIdDraft: (value: string) => void;
  diaryPantryGramsDraft: string;
  setDiaryPantryGramsDraft: (value: string) => void;
  bumpPantryGrams: (delta: number) => void;
  onSavePantry: () => Promise<void>;
  selectedDiaryPantryCoffee: CoffeeRow | null;
  createCoffeeFormContent?: ReactNode;
  onCreateCoffeeNext?: () => Promise<void>;
  createCoffeeFormForPantrySheet?: ReactNode;
  onCreateCoffeeNextForPantry?: () => Promise<void>;
  isCreateCoffeeFormValid?: boolean;
  showBarcodeButton?: boolean;
  onBarcodeClick?: () => void;
  barcodeDetectedValue?: string | null;
  onClearBarcodeDetectedValue?: () => void;
}) {
  const [coffeeSearchQuery, setCoffeeSearchQuery] = useState("");
  const [coffeeSearchFocus, setCoffeeSearchFocus] = useState(false);
  const [pantrySearchQuery, setPantrySearchQuery] = useState("");
  const showSearchCancel = coffeeSearchQuery !== "" || coffeeSearchFocus;

  useEffect(() => {
    if (showCoffeeSheet) {
      setCoffeeSheetStep("select");
      setDiaryCoffeePreparationDraft("Espresso");
    }
  }, [showCoffeeSheet, setCoffeeSheetStep, setDiaryCoffeePreparationDraft]);

  useEffect(() => {
    if (barcodeDetectedValue != null && barcodeDetectedValue !== "") {
      setCoffeeSearchQuery(barcodeDetectedValue);
      onClearBarcodeDetectedValue?.();
    }
  }, [barcodeDetectedValue, onClearBarcodeDetectedValue]);

  useEffect(() => {
    if (showAddPantrySheet && pantrySheetStep === "select" && barcodeDetectedValue != null && barcodeDetectedValue !== "") {
      setPantrySearchQuery(barcodeDetectedValue);
      onClearBarcodeDetectedValue?.();
    }
  }, [showAddPantrySheet, pantrySheetStep, barcodeDetectedValue, onClearBarcodeDetectedValue]);

  useEffect(() => {
    if (showAddPantrySheet) {
      setPantrySearchQuery("");
    }
  }, [showAddPantrySheet]);

  const userPantryRows = useMemo(
    () => (activeUserId != null ? pantryCoffeeRows.filter((r) => r.item.user_id === activeUserId) : []),
    [pantryCoffeeRows, activeUserId]
  );
  const filteredCoffeeSuggestions = useMemo(() => {
    const q = coffeeSearchQuery.trim().toLowerCase();
    const filtered = q
      ? diaryCoffeeOptions.filter(
          (c) => c.nombre.toLowerCase().includes(q) || (c.marca ?? "").toLowerCase().includes(q)
        )
      : diaryCoffeeOptions;
    return filtered.slice(0, 10);
  }, [diaryCoffeeOptions, coffeeSearchQuery]);

  const filteredPantryCoffees = useMemo(() => {
    const q = pantrySearchQuery.trim().toLowerCase();
    return q
      ? diaryCoffeeOptions.filter(
          (c) => c.nombre.toLowerCase().includes(q) || (c.marca ?? "").toLowerCase().includes(q)
        )
      : diaryCoffeeOptions;
  }, [diaryCoffeeOptions, pantrySearchQuery]);

  const selectedCoffeeForDose = useMemo(
    () => (diaryCoffeeIdDraft ? diaryCoffeeOptions.find((c) => c.id === diaryCoffeeIdDraft) : null),
    [diaryCoffeeIdDraft, diaryCoffeeOptions]
  );
  const doseCoffeeName = selectedCoffeeForDose?.nombre ?? "Registro rápido";

  const doseSliderMin = 5;
  const doseSliderMax = 30;
  const doseSliderStep = 0.5;

  if (!isActive) return null;

  return (
    <>
      {showQuickActions ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Nuevo registro" onDismiss={onCloseQuickActions} onClick={onCloseQuickActions}>
          <SheetCard className="diary-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">NUEVO REGISTRO</strong>
            </header>
            <div className="diary-sheet-list">
              <Button variant="plain" type="button" className="diary-sheet-action is-water" onClick={onOpenWaterSheet}>
                <UiIcon name="taste-watery" className="ui-icon" />
                <span>Agua</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </Button>
              <Button variant="plain" type="button" className="diary-sheet-action is-coffee" onClick={onOpenCoffeeSheet}>
                <UiIcon name="coffee-filled" className="ui-icon" />
                <span>Café</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </Button>
              <Button variant="plain" type="button" className="diary-sheet-action is-pantry" onClick={onOpenAddPantrySheet}>
                <UiIcon name="add" className="ui-icon" />
                <span>Añadir a Despensa</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </Button>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showPeriodSheet ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Seleccionar periodo" onDismiss={onClosePeriodSheet} onClick={onClosePeriodSheet}>
          <SheetCard className="diary-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">SELECCIONAR PERIODO</strong>
            </header>
            <div className="diary-sheet-list">
              {([
                { value: "hoy", label: "HOY" },
                { value: "7d", label: "SEMANA" },
                { value: "30d", label: "MES" }
              ] as const).map((option) => (
                <Button variant="plain"
                  key={option.value}
                  type="button"
                  className={`diary-sheet-action ${diaryPeriod === option.value ? "is-active" : ""}`.trim()}
                  onClick={() => {
                    setDiaryPeriod(option.value);
                    onClosePeriodSheet();
                  }}
                >
                  <span>{option.label}</span>
                </Button>
              ))}
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showWaterSheet ? (
        <SheetOverlay className="diary-water-sheet-overlay" role="dialog" aria-modal="true" aria-label="Registrar agua" onDismiss={onCloseWaterSheet} onClick={onCloseWaterSheet}>
          <SheetCard className="diary-sheet diary-water-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header diary-water-sheet-header">
              <strong className="sheet-title">Agua</strong>
              <Button variant="plain" type="button" className="action-button diary-water-sheet-register-btn" onClick={() => void onSaveWater()}>
                Registrar
              </Button>
            </header>
            <div className="diary-sheet-form">
              <div className="diary-water-card">
                <div className="diary-water-card-icon-wrap">
                  <UiIcon name="taste-watery" className="ui-icon diary-water-drop-icon" aria-hidden="true" />
                </div>
                <div className="diary-water-card-input-wrap">
                  <label className="diary-water-card-input-row">
                    <Input
                      className="diary-water-card-input"
                      type="number"
                      inputMode="numeric"
                      min={50}
                      max={1000}
                      value={diaryWaterMlDraft}
                      onChange={(event) => {
                        const v = event.target.value;
                        if (v === "" || /^\d+$/.test(v)) setDiaryWaterMlDraft(v);
                      }}
                      aria-label="Cantidad en ml"
                    />
                    <span className="diary-water-card-unit" aria-hidden="true">ml</span>
                  </label>
                </div>
              </div>
              {(() => {
                const sliderVal = Math.max(50, Math.min(1000, Number(diaryWaterMlDraft) || 250));
                const waterProgressStyle: Record<string, string> = {
                  "--water-progress": `${((sliderVal - 50) / 950) * 100}%`,
                };
                return (
                  <div className="diary-water-slider-wrap">
                    <input
                      type="range"
                      className="diary-water-slider"
                      min={50}
                      max={1000}
                      step={10}
                      value={sliderVal}
                      onChange={(event) => setDiaryWaterMlDraft(event.target.value)}
                      aria-label="Cantidad de agua en ml"
                      style={waterProgressStyle}
                    />
                    <span className="diary-water-slider-end-dot" aria-hidden="true" />
                  </div>
                );
              })()}
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showCoffeeSheet ? (
        <SheetOverlay className="diary-coffee-sheet-overlay" role="dialog" aria-modal="true" aria-label="Seleccionar café" onDismiss={onCloseCoffeeSheet} onClick={onCloseCoffeeSheet}>
          <SheetCard className="diary-sheet diary-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            {coffeeSheetStep === "select" ? (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-coffee-select-header">
                  <Button variant="plain" type="button" className="diary-coffee-select-close" onClick={onCloseCoffeeSheet} aria-label="Cerrar">
                    <UiIcon name="close" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">SELECCIONA</strong>
                  <Button
                    variant="plain"
                    type="button"
                    className="diary-coffee-select-action"
                    aria-label="Registro rápido"
                    onClick={() => {
                      setDiaryCoffeeIdDraft("");
                      setCoffeeSheetStep("dose");
                    }}
                  >
                    <UiIcon name="bolt" className="ui-icon" />
                  </Button>
                </header>
                <div className="diary-coffee-select-body">
                  <section className="diary-coffee-select-section">
                    <h3 className="diary-coffee-select-section-title">TU DESPENSA</h3>
                    {userPantryRows.length === 0 ? (
                      <div className="diary-coffee-select-pantry-empty">
                        <p>Tu despensa está vacía</p>
                      </div>
                    ) : (
                      <div className="diary-coffee-select-pantry-row">
                        {userPantryRows.map((row) => (
                          <Button
                            variant="plain"
                            key={row.item.coffee_id}
                            type="button"
                            className="diary-coffee-select-pantry-card"
                            onClick={() => {
                              setDiaryCoffeeIdDraft(row.coffee.id);
                              setCoffeeSheetStep("dose");
                            }}
                          >
                            <span className="diary-coffee-select-pantry-card-img">
                              {row.coffee.image_url ? (
                                <img src={row.coffee.image_url} alt="" loading="lazy" decoding="async" />
                              ) : (
                                <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" decoding="async" />
                              )}
                            </span>
                            <div className="diary-coffee-select-pantry-copy">
                              <strong>{row.coffee.nombre}</strong>
                              <span>{Math.round(row.item.grams_remaining)}G REST.</span>
                            </div>
                          </Button>
                        ))}
                      </div>
                    )}
                  </section>
                  <div className={`search-row-with-cancel diary-coffee-select-search-row ${showSearchCancel ? "has-cancel" : ""}`.trim()}>
                    <div className="search-coffee-field">
                      <UiIcon name="search" className="ui-icon search-coffee-leading-icon" aria-hidden="true" />
                      <Input
                        variant="search"
                        type="search"
                        className="search-coffee-input"
                        placeholder="Busca un café o marca"
                        value={coffeeSearchQuery}
                        onFocus={() => setCoffeeSearchFocus(true)}
                        onBlur={() => setCoffeeSearchFocus(false)}
                        onChange={(event) => setCoffeeSearchQuery(event.target.value)}
                        aria-label="Buscar café o marca"
                      />
                      {showBarcodeButton && onBarcodeClick ? (
                        <Button
                          variant="plain"
                          type="button"
                          className="search-coffee-trailing-button"
                          aria-label="Escanear código de barras"
                          onClick={() => onBarcodeClick()}
                        >
                          <UiIcon name="barcode" className="ui-icon" />
                        </Button>
                      ) : null}
                    </div>
                    <Button
                      variant="text"
                      type="button"
                      className={`search-cancel-button ${showSearchCancel ? "is-visible" : ""}`.trim()}
                      onClick={() => {
                        setCoffeeSearchQuery("");
                        setCoffeeSearchFocus(false);
                        const active = document.activeElement;
                        if (active instanceof HTMLElement) active.blur();
                      }}
                      aria-hidden={!showSearchCancel}
                      tabIndex={showSearchCancel ? 0 : -1}
                    >
                      Cancelar
                    </Button>
                  </div>
                  <section className="diary-coffee-select-section">
                    <div className="diary-coffee-select-section-head">
                      <h3 className="diary-coffee-select-section-title">SUGERENCIAS</h3>
                      <Button
                        variant="plain"
                        type="button"
                        className="diary-coffee-select-create-btn"
                        onClick={() => setCoffeeSheetStep("createCoffee")}
                      >
                        <UiIcon name="add" className="ui-icon" />
                        <span>Crear mi café</span>
                      </Button>
                    </div>
                    <ul className="diary-coffee-select-list" role="listbox" aria-label="Sugerencias de café">
                      {filteredCoffeeSuggestions.map((coffee) => (
                        <li key={coffee.id}>
                          <Button
                            variant="plain"
                            type="button"
                            role="option"
                            className="diary-coffee-select-item"
                            onClick={() => {
                              setDiaryCoffeeIdDraft(coffee.id);
                              setCoffeeSheetStep("dose");
                            }}
                          >
                            {coffee.image_url ? (
                              <img src={coffee.image_url} alt="" loading="lazy" decoding="async" />
                            ) : (
                              <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" decoding="async" />
                            )}
                            <div className="diary-coffee-select-item-copy">
                              <strong>{coffee.nombre}</strong>
                              <span>{(coffee.marca || "CAFÉ").toUpperCase()}</span>
                            </div>
                          </Button>
                        </li>
                      ))}
                    </ul>
                  </section>
                </div>
              </>
            ) : coffeeSheetStep === "createCoffee" ? (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-create-coffee-sheet-header">
                  <Button variant="plain" type="button" className="diary-create-coffee-back" onClick={() => setCoffeeSheetStep("select")} aria-label="Volver">
                    <UiIcon name="arrow-left" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">DATOS DEL CAFÉ</strong>
                  <Button
                    variant="plain"
                    type="button"
                    className="diary-create-coffee-next"
                    onClick={() => void onCreateCoffeeNext?.()}
                    aria-label="Siguiente"
                  >
                    <UiIcon name="arrow-right" className="ui-icon" />
                  </Button>
                </header>
                <div className="diary-sheet-form diary-create-coffee-sheet-body">
                  {createCoffeeFormContent}
                </div>
              </>
            ) : coffeeSheetStep === "dose" ? (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-dose-sheet-header">
                  <Button variant="plain" type="button" className="diary-dose-back" onClick={() => setCoffeeSheetStep("select")} aria-label="Volver">
                    <UiIcon name="arrow-left" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">DOSIS</strong>
                  <Button variant="plain" type="button" className="diary-dose-next" onClick={() => setCoffeeSheetStep("tipo")} aria-label="Siguiente">
                    <UiIcon name="arrow-right" className="ui-icon" />
                  </Button>
                </header>
                <div className="diary-sheet-form diary-dose-sheet-form">
                  <div className="diary-dose-card">
                    <div className="diary-dose-card-icon-wrap">
                      <UiIcon name="coffee-filled" className="ui-icon diary-dose-cup-icon" aria-hidden="true" />
                    </div>
                    <p className="diary-dose-coffee-name">{doseCoffeeName}</p>
                    <div className="diary-dose-card-input-wrap">
                      <label className="diary-dose-card-input-row">
                        <Input
                          className="diary-dose-card-input"
                          type="number"
                          inputMode="decimal"
                          min={doseSliderMin}
                          max={doseSliderMax}
                          step={doseSliderStep}
                          value={diaryCoffeeGramsDraft}
                          onChange={(event) => {
                            const v = event.target.value;
                            if (v === "" || /^\d*[,.]?\d*$/.test(v.replace(",", "."))) setDiaryCoffeeGramsDraft(v.replace(",", "."));
                          }}
                          aria-label="Dosis en gramos"
                        />
                        <span className="diary-dose-card-unit" aria-hidden="true">g</span>
                      </label>
                    </div>
                  </div>
                  {(() => {
                    const sliderVal = Math.max(doseSliderMin, Math.min(doseSliderMax, Number(diaryCoffeeGramsDraft) || 15));
                    const doseProgressStyle: Record<string, string> = {
                      "--dose-progress": `${((sliderVal - doseSliderMin) / (doseSliderMax - doseSliderMin)) * 100}%`,
                    };
                    return (
                      <div className="diary-dose-slider-wrap">
                        <input
                          type="range"
                          className="diary-dose-slider"
                          min={doseSliderMin}
                          max={doseSliderMax}
                          step={doseSliderStep}
                          value={sliderVal}
                          onChange={(event) => setDiaryCoffeeGramsDraft(event.target.value)}
                          aria-label="Dosis en gramos"
                          style={doseProgressStyle}
                        />
                        <span className="diary-dose-slider-end-dot" aria-hidden="true" />
                      </div>
                    );
                  })()}
                </div>
              </>
            ) : coffeeSheetStep === "tipo" ? (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-dose-sheet-header">
                  <Button variant="plain" type="button" className="diary-dose-back" onClick={() => setCoffeeSheetStep("dose")} aria-label="Volver">
                    <UiIcon name="arrow-left" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">TIPO</strong>
                  <Button variant="plain" type="button" className="diary-dose-next" onClick={() => { setDiaryCoffeeMlDraft("275"); setCoffeeSheetStep("tamaño"); }} aria-label="Siguiente">
                    <UiIcon name="arrow-right" className="ui-icon" />
                  </Button>
                </header>
                <div className="diary-sheet-form diary-tipo-sheet-form">
                  <div className="diary-tipo-grid" role="listbox" aria-label="Tipo de café">
                    {[
                      { label: "Espresso", drawable: "espresso.png" },
                      { label: "Americano", drawable: "americano.png" },
                      { label: "Capuchino", drawable: "capuchino.png" },
                      { label: "Latte", drawable: "latte.png" },
                      { label: "Macchiato", drawable: "macchiato.png" },
                      { label: "Moca", drawable: "moca.png" },
                      { label: "Vienés", drawable: "vienes.png" },
                      { label: "Irlandés", drawable: "irlandes.png" },
                      { label: "Frappuccino", drawable: "frappuccino.png" },
                      { label: "Caramelo macchiato", drawable: "caramel_macchiato.png" },
                      { label: "Corretto", drawable: "corretto.png" },
                      { label: "Freddo", drawable: "freddo.png" },
                      { label: "Latte macchiato", drawable: "latte_macchiato.png" },
                      { label: "Leche con chocolate", drawable: "leche_con_chocolate.png" },
                      { label: "Marroquí", drawable: "marroqui.png" },
                      { label: "Romano", drawable: "romano.png" },
                      { label: "Descafeinado", drawable: "descafeinado.png" }
                    ].map(({ label, drawable }) => {
                      const isSelected = diaryCoffeePreparationDraft === label;
                      return (
                        <Button
                          variant="plain"
                          key={label}
                          type="button"
                          role="option"
                          aria-selected={isSelected}
                          className={`diary-tipo-card ${isSelected ? "is-active" : ""}`.trim()}
                          onClick={() => setDiaryCoffeePreparationDraft(label)}
                        >
                          <span className="diary-tipo-card-icon">
                            <img src={`/android-drawable/${drawable}`} alt="" aria-hidden="true" loading="lazy" decoding="async" />
                          </span>
                          <span className="diary-tipo-card-label">{label}</span>
                        </Button>
                      );
                    })}
                  </div>
                </div>
              </>
            ) : coffeeSheetStep === "tamaño" ? (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-tamaño-sheet-header">
                  <Button variant="plain" type="button" className="diary-dose-back" onClick={() => setCoffeeSheetStep("tipo")} aria-label="Volver">
                    <UiIcon name="arrow-left" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">TAMAÑO</strong>
                  <Button
                    variant="plain"
                    type="button"
                    className="diary-tamaño-register"
                    onClick={() => {
                      const isRegistroRapido = !diaryCoffeeIdDraft || diaryCoffeeIdDraft === "";
                      const selected = isRegistroRapido ? null : diaryCoffeeOptions.find((c) => c.id === diaryCoffeeIdDraft);
                      const coffeeName = selected?.nombre ?? lastCreatedCoffeeNameForSheet ?? "Registro rápido";
                      const ml = Math.max(1, Number(diaryCoffeeMlDraft || 0));
                      const sizeOptions: { ml: number; label: string }[] = [
                        { ml: 30, label: "Espresso" },
                        { ml: 180, label: "Pequeño" },
                        { ml: 275, label: "Mediano" },
                        { ml: 375, label: "Grande" },
                        { ml: 475, label: "Tazón XL" }
                      ];
                      const sizeLabel = sizeOptions.find((s) => Math.abs(s.ml - ml) <= 15)?.label ?? null;
                      void onSaveCoffee({
                        coffeeId: isRegistroRapido ? null : (diaryCoffeeIdDraft || null),
                        coffeeName,
                        coffeeBrand: selected?.marca ?? "",
                        amountMl: ml,
                        caffeineMg: Math.max(0, Number(diaryCoffeeCaffeineDraft || 0)),
                        coffeeGrams: Math.max(0, Math.round(Number(diaryCoffeeGramsDraft || 0))),
                        preparationType: diaryCoffeePreparationDraft.trim() || "Manual",
                        sizeLabel
                      });
                    }}
                    aria-label="Registrar"
                  >
                    REGISTRAR
                  </Button>
                </header>
                <div className="diary-sheet-form diary-tamaño-sheet-form">
                  <div className="diary-tamaño-list" role="listbox" aria-label="Tamaño de la taza">
                    {[
                      { label: "Espresso", rangeLabel: "25–30 ml", ml: 30, drawable: "taza_espresso.png" },
                      { label: "Pequeño", rangeLabel: "150–200 ml", ml: 180, drawable: "taza_pequeno.png" },
                      { label: "Mediano", rangeLabel: "250–300 ml", ml: 275, drawable: "taza_mediano.png" },
                      { label: "Grande", rangeLabel: "350–400 ml", ml: 375, drawable: "taza_grande.png" },
                      { label: "Tazón XL", rangeLabel: "450–500 ml", ml: 475, drawable: "taza_xl.png" }
                    ].map((size) => {
                      const currentMl = Number(diaryCoffeeMlDraft || 0);
                      const isSelected = Math.abs(currentMl - size.ml) <= 15;
                      return (
                        <Button
                          variant="plain"
                          key={size.label}
                          type="button"
                          role="option"
                          aria-selected={isSelected}
                          className={`diary-tamaño-card ${isSelected ? "is-active" : ""}`.trim()}
                          onClick={() => setDiaryCoffeeMlDraft(String(size.ml))}
                        >
                          <span className="diary-tamaño-card-icon">
                            <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" loading="lazy" decoding="async" />
                          </span>
                          <span className="diary-tamaño-card-label">{size.label}</span>
                          <span className="diary-tamaño-card-range">{size.rangeLabel}</span>
                        </Button>
                      );
                    })}
                  </div>
                </div>
              </>
            ) : null}
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showAddPantrySheet ? (
        <SheetOverlay className="diary-pantry-sheet-overlay" role="dialog" aria-modal="true" aria-label="Añadir a despensa" onDismiss={onCloseAddPantrySheet} onClick={onCloseAddPantrySheet}>
          <SheetCard className="diary-sheet diary-pantry-sheet" onClick={(event) => event.stopPropagation()}>
            {pantrySheetStep === "select" ? (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-pantry-select-header">
                  <Button variant="plain" type="button" className="diary-pantry-select-back" onClick={onCloseAddPantrySheet} aria-label="Cerrar">
                    <UiIcon name="arrow-left" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">SELECCIONAR</strong>
                  <Button
                    variant="plain"
                    type="button"
                    className="diary-pantry-select-action"
                    aria-label="Crear café"
                    onClick={() => setPantrySheetStep("createCoffee")}
                  >
                    <UiIcon name="bolt" className="ui-icon" />
                  </Button>
                </header>
                <div className="diary-pantry-select-body">
                  <div className="diary-pantry-search-row">
                    <div className="search-coffee-field">
                      <UiIcon name="search" className="ui-icon search-coffee-leading-icon" aria-hidden="true" />
                      <Input
                        variant="search"
                        type="search"
                        className="search-coffee-input"
                        placeholder="Busca un café o marca"
                        value={pantrySearchQuery}
                        onChange={(event) => setPantrySearchQuery(event.target.value)}
                        aria-label="Buscar café o marca"
                      />
                      {showBarcodeButton && onBarcodeClick ? (
                        <Button variant="plain" type="button" className="search-coffee-trailing-button" aria-label="Escanear código de barras" onClick={() => onBarcodeClick()}>
                          <UiIcon name="barcode" className="ui-icon" />
                        </Button>
                      ) : null}
                    </div>
                  </div>
                  <ul className="diary-pantry-select-list" role="listbox" aria-label="Seleccionar café para despensa">
                    {filteredPantryCoffees.map((coffee) => (
                      <li key={coffee.id}>
                        <Button
                          variant="plain"
                          type="button"
                          role="option"
                          className="diary-pantry-select-card"
                          onClick={() => {
                            setDiaryPantryCoffeeIdDraft(coffee.id);
                            setPantrySheetStep("form");
                          }}
                        >
                          <span className="diary-pantry-select-card-img">
                            {coffee.image_url ? (
                              <img src={coffee.image_url} alt="" loading="lazy" decoding="async" />
                            ) : (
                              <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" decoding="async" />
                            )}
                          </span>
                          <div className="diary-pantry-select-card-copy">
                            <strong>{coffee.nombre}</strong>
                            <span>{(coffee.marca || "CAFÉ").toUpperCase()}</span>
                          </div>
                        </Button>
                      </li>
                    ))}
                  </ul>
                </div>
              </>
            ) : pantrySheetStep === "createCoffee" ? (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-pantry-create-coffee-header">
                  <Button variant="plain" type="button" className="diary-pantry-create-coffee-back" onClick={() => setPantrySheetStep("select")} aria-label="Volver">
                    <UiIcon name="arrow-left" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">NUEVO CAFÉ</strong>
                  <Button
                    variant="plain"
                    type="button"
                    className="diary-pantry-create-coffee-next"
                    disabled={!isCreateCoffeeFormValid}
                    onClick={() => void onCreateCoffeeNextForPantry?.()}
                    aria-label="Añadir a despensa"
                  >
                    Añadir
                  </Button>
                </header>
                <div className="diary-sheet-form diary-pantry-create-coffee-body">
                  {createCoffeeFormForPantrySheet}
                </div>
              </>
            ) : (
              <>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-pantry-form-header">
                  <Button variant="plain" type="button" className="diary-pantry-form-back" onClick={() => setPantrySheetStep("select")} aria-label="Volver">
                    <UiIcon name="arrow-left" className="ui-icon" />
                  </Button>
                  <strong className="sheet-title">AÑADIR A DESPENSA</strong>
                  <span aria-hidden="true" />
                </header>
                <div className="diary-sheet-form">
                  {selectedDiaryPantryCoffee ? (
                    <div className="diary-edit-entry-preview" aria-hidden="true">
                      <div className="diary-entry-media">
                        {selectedDiaryPantryCoffee.image_url ? (
                          <img src={selectedDiaryPantryCoffee.image_url} alt={selectedDiaryPantryCoffee.nombre} loading="lazy" decoding="async" />
                        ) : (
                          <img className="diary-entry-fallback-drawable" src="/android-drawable/taza_mediano.png" alt="" aria-hidden="true" loading="lazy" decoding="async" />
                        )}
                      </div>
                      <div className="diary-entry-copy">
                        <p className="feed-user">{selectedDiaryPantryCoffee.nombre}</p>
                        <p className="feed-meta diary-entry-brand">{(selectedDiaryPantryCoffee.marca || "CAFÉ").toUpperCase()}</p>
                      </div>
                    </div>
                  ) : null}
                  <div className="diary-water-presets diary-edit-entry-presets is-water">
                    {[100, 250, 500].map((value) => (
                      <Button variant="plain"
                        key={value}
                        type="button"
                        className={`chip-button period-chip ${Number(diaryPantryGramsDraft) === value ? "is-active" : ""}`.trim()}
                        onClick={() => setDiaryPantryGramsDraft(String(value))}
                      >
                        {value} g
                      </Button>
                    ))}
                  </div>
                  <label>
                    <span>Gramos a añadir</span>
                    <div className="diary-edit-entry-measure">
                      <Button variant="plain" type="button" className="diary-edit-entry-step" aria-label="Reducir gramos" onClick={() => bumpPantryGrams(-25)}>
                        -
                      </Button>
                      <Input
                        className="search-wide diary-edit-entry-input"
                        type="number"
                        inputMode="numeric"
                        min={1}
                        value={diaryPantryGramsDraft}
                        onChange={(event) => setDiaryPantryGramsDraft(event.target.value)}
                      />
                      <span className="diary-edit-entry-unit" aria-hidden="true">g</span>
                      <Button variant="plain" type="button" className="diary-edit-entry-step" aria-label="Aumentar gramos" onClick={() => bumpPantryGrams(25)}>
                        +
                      </Button>
                    </div>
                    <Input
                      className="diary-edit-entry-slider"
                      type="range"
                      min={1}
                      max={2000}
                      step={25}
                      value={Math.max(1, Number(diaryPantryGramsDraft || 1))}
                      onChange={(event) => setDiaryPantryGramsDraft(String(Math.max(1, Number(event.target.value || 1))))}
                    />
                  </label>
                  <div className="diary-sheet-form-actions">
                    <Button variant="plain" type="button" className="action-button action-button-ghost diary-edit-entry-cancel" onClick={onCloseAddPantrySheet}>
                      Cancelar
                    </Button>
                    <Button variant="plain" type="button" className="action-button diary-edit-entry-save" disabled={!selectedDiaryPantryCoffee} onClick={() => void onSavePantry()}>
                      Guardar
                    </Button>
                  </div>
                </div>
              </>
            )}
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </>
  );
}





export { DiarySheets };
export default DiarySheets;

