import type { CoffeeRow } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { Button, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
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
  diaryCoffeeOptions,
  diaryCoffeeIdDraft,
  setDiaryCoffeeIdDraft,
  diaryCoffeePreparationDraft,
  setDiaryCoffeePreparationDraft,
  diaryCoffeeMlDraft,
  setDiaryCoffeeMlDraft,
  diaryCoffeeCaffeineDraft,
  setDiaryCoffeeCaffeineDraft,
  onSaveCoffee,
  diaryPantryCoffeeIdDraft,
  setDiaryPantryCoffeeIdDraft,
  diaryPantryGramsDraft,
  setDiaryPantryGramsDraft,
  bumpPantryGrams,
  onSavePantry,
  selectedDiaryPantryCoffee
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
  diaryCoffeeOptions: CoffeeRow[];
  diaryCoffeeIdDraft: string;
  setDiaryCoffeeIdDraft: (value: string) => void;
  diaryCoffeePreparationDraft: string;
  setDiaryCoffeePreparationDraft: (value: string) => void;
  diaryCoffeeMlDraft: string;
  setDiaryCoffeeMlDraft: (value: string) => void;
  diaryCoffeeCaffeineDraft: string;
  setDiaryCoffeeCaffeineDraft: (value: string) => void;
  onSaveCoffee: () => Promise<void>;
  diaryPantryCoffeeIdDraft: string;
  setDiaryPantryCoffeeIdDraft: (value: string) => void;
  diaryPantryGramsDraft: string;
  setDiaryPantryGramsDraft: (value: string) => void;
  bumpPantryGrams: (delta: number) => void;
  onSavePantry: () => Promise<void>;
  selectedDiaryPantryCoffee: CoffeeRow | null;
}) {
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
                <UiIcon name="stock" className="ui-icon" />
                <span>Agua</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </Button>
              <Button variant="plain" type="button" className="diary-sheet-action is-coffee" onClick={onOpenCoffeeSheet}>
                <UiIcon name="coffee" className="ui-icon" />
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
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Registrar agua" onDismiss={onCloseWaterSheet} onClick={onCloseWaterSheet}>
          <SheetCard className="diary-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">REGISTRAR AGUA</strong>
            </header>
            <div className="diary-sheet-form">
              <div className="diary-water-presets">
                {[250, 500, 750].map((value) => (
                  <Button variant="plain"
                    key={value}
                    type="button"
                    className={`chip-button period-chip ${Number(diaryWaterMlDraft) === value ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryWaterMlDraft(String(value))}
                  >
                    {value} ml
                  </Button>
                ))}
              </div>
              <label>
                <span>Cantidad personalizada (ml)</span>
                <Input
                  className="search-wide"
                  type="number"
                  min={1}
                  value={diaryWaterMlDraft}
                  onChange={(event) => setDiaryWaterMlDraft(event.target.value)}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <Button variant="plain" type="button" className="action-button action-button-ghost" onClick={onCloseWaterSheet}>
                  Cancelar
                </Button>
                <Button variant="plain" type="button" className="action-button" onClick={() => void onSaveWater()}>
                  Guardar
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showCoffeeSheet ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Registrar cafe" onDismiss={onCloseCoffeeSheet} onClick={onCloseCoffeeSheet}>
          <SheetCard className="diary-sheet diary-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">REGISTRAR CAFÉ</strong>
            </header>
            <div className="diary-sheet-form">
              <div className="diary-edit-entry-presets diary-coffee-method-presets">
                {[
                  { label: "Espresso", drawable: "maq_espresso.png" },
                  { label: "V60", drawable: "maq_hario_v60.png" },
                  { label: "Aeropress", drawable: "maq_aeropress.png" },
                  { label: "Moka", drawable: "maq_italiana.png" }
                ].map((method) => (
                  <Button variant="plain"
                    key={method.label}
                    type="button"
                    className={`chip-button period-chip diary-edit-entry-method-chip ${diaryCoffeePreparationDraft.toLowerCase() === method.label.toLowerCase() ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryCoffeePreparationDraft(method.label)}
                  >
                    <img src={`/android-drawable/${method.drawable}`} alt="" aria-hidden="true" />
                    <span>{method.label}</span>
                  </Button>
                ))}
              </div>
              <label>
                <span>Café</span>
                <div className="diary-coffee-picker" role="listbox" aria-label="Seleccionar café">
                  {diaryCoffeeOptions.map((coffee) => {
                    const selected = coffee.id === diaryCoffeeIdDraft;
                    return (
                      <Button variant="plain"
                        key={coffee.id}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={`diary-coffee-picker-item ${selected ? "is-active" : ""}`.trim()}
                        onClick={() => setDiaryCoffeeIdDraft(coffee.id)}
                      >
                        <span className="diary-coffee-picker-media">
                          {coffee.image_url ? (
                            <img src={coffee.image_url} alt="" loading="lazy" />
                          ) : (
                            <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" />
                          )}
                        </span>
                        <span className="diary-coffee-picker-copy">
                          <strong>{coffee.nombre}</strong>
                          <em>{(coffee.marca || "CAFÉ").toUpperCase()}</em>
                        </span>
                      </Button>
                    );
                  })}
                </div>
              </label>
              <label>
                <span>Preparación</span>
                <Input className="search-wide" value={diaryCoffeePreparationDraft} onChange={(event) => setDiaryCoffeePreparationDraft(event.target.value)} />
              </label>
              <label>
                <span>Cantidad (ml)</span>
                <Input
                  className="search-wide diary-edit-entry-input"
                  type="number"
                  inputMode="numeric"
                  min={1}
                  value={diaryCoffeeMlDraft}
                  onChange={(event) => setDiaryCoffeeMlDraft(event.target.value)}
                />
                <Input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={10}
                  max={700}
                  step={10}
                  value={Math.max(10, Number(diaryCoffeeMlDraft || 10))}
                  onChange={(event) => setDiaryCoffeeMlDraft(String(Math.max(10, Number(event.target.value || 10))))}
                />
              </label>
              <div className="diary-coffee-size-presets">
                {[
                  { label: "Espresso", ml: 30, drawable: "taza_espresso.png" },
                  { label: "Peq.", ml: 180, drawable: "taza_pequeno.png" },
                  { label: "Med.", ml: 275, drawable: "taza_mediano.png" },
                  { label: "Gra.", ml: 375, drawable: "taza_grande.png" },
                  { label: "XL", ml: 475, drawable: "taza_xl.png" }
                ].map((size) => (
                  <Button variant="plain"
                    key={size.label}
                    type="button"
                    className={`chip-button period-chip diary-coffee-size-chip ${Math.abs(Number(diaryCoffeeMlDraft || 0) - size.ml) <= 10 ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryCoffeeMlDraft(String(size.ml))}
                  >
                    <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" />
                    <span>{size.label}</span>
                  </Button>
                ))}
              </div>
              <label>
                <span>Cafeína (mg)</span>
                <Input
                  className="search-wide diary-edit-entry-input"
                  type="number"
                  inputMode="numeric"
                  min={0}
                  value={diaryCoffeeCaffeineDraft}
                  onChange={(event) => setDiaryCoffeeCaffeineDraft(event.target.value)}
                />
                <Input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={0}
                  max={350}
                  step={5}
                  value={Math.max(0, Number(diaryCoffeeCaffeineDraft || 0))}
                  onChange={(event) => setDiaryCoffeeCaffeineDraft(String(Math.max(0, Number(event.target.value || 0))))}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <Button variant="plain" type="button" className="action-button action-button-ghost" onClick={onCloseCoffeeSheet}>
                  Cancelar
                </Button>
                <Button variant="plain" type="button" className="action-button" onClick={() => void onSaveCoffee()}>
                  Guardar
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showAddPantrySheet ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Añadir a despensa" onDismiss={onCloseAddPantrySheet} onClick={onCloseAddPantrySheet}>
          <SheetCard className="diary-sheet diary-pantry-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">AÑADIR A DESPENSA</strong>
            </header>
            <div className="diary-sheet-form">
              {selectedDiaryPantryCoffee ? (
                <div className="diary-edit-entry-preview" aria-hidden="true">
                  <div className="diary-entry-media">
                    {selectedDiaryPantryCoffee.image_url ? (
                      <img src={selectedDiaryPantryCoffee.image_url} alt={selectedDiaryPantryCoffee.nombre} loading="lazy" />
                    ) : (
                      <img className="diary-entry-fallback-drawable" src="/android-drawable/taza_mediano.png" alt="" aria-hidden="true" loading="lazy" />
                    )}
                  </div>
                  <div className="diary-entry-copy">
                    <p className="feed-user">{selectedDiaryPantryCoffee.nombre}</p>
                    <p className="feed-meta diary-entry-brand">{(selectedDiaryPantryCoffee.marca || "CAFÉ").toUpperCase()}</p>
                  </div>
                </div>
              ) : null}
              <label>
                <span>Café</span>
                <div className="diary-coffee-picker" role="listbox" aria-label="Seleccionar café para despensa">
                  {diaryCoffeeOptions.map((coffee) => {
                    const selected = coffee.id === diaryPantryCoffeeIdDraft;
                    return (
                      <Button variant="plain"
                        key={coffee.id}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={`diary-coffee-picker-item ${selected ? "is-active" : ""}`.trim()}
                        onClick={() => setDiaryPantryCoffeeIdDraft(coffee.id)}
                      >
                        <span className="diary-coffee-picker-media">
                          {coffee.image_url ? (
                            <img src={coffee.image_url} alt="" loading="lazy" />
                          ) : (
                            <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" />
                          )}
                        </span>
                        <span className="diary-coffee-picker-copy">
                          <strong>{coffee.nombre}</strong>
                          <em>{(coffee.marca || "CAFÉ").toUpperCase()}</em>
                        </span>
                      </Button>
                    );
                  })}
                </div>
              </label>
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
                <Button variant="plain" type="button" className="action-button diary-edit-entry-save" onClick={() => void onSavePantry()}>
                  Guardar
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </>
  );
}





export { DiarySheets };
export default DiarySheets;

