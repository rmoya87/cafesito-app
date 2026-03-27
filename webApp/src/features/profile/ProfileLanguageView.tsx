import { UiIcon } from "../../ui/iconography";
import { useI18n, type LocalePreference } from "../../i18n";

const EXPLICIT_LANGUAGES: Array<{ value: Exclude<LocalePreference, "system">; key: "language.es" | "language.en" | "language.fr" | "language.pt" | "language.de" }> = [
  { value: "es", key: "language.es" },
  { value: "en", key: "language.en" },
  { value: "fr", key: "language.fr" },
  { value: "pt", key: "language.pt" },
  { value: "de", key: "language.de" }
];

export function ProfileLanguageView() {
  const { t, localePreference, setLocalePreference } = useI18n();
  const languageRows: Array<{
    value: LocalePreference;
    label: string;
    description?: string;
  }> = [
    { value: "system", label: t("language.system") },
    ...EXPLICIT_LANGUAGES.map((lang) => ({ value: lang.value, label: t(lang.key) }))
  ];

  return (
    <section className="profile-lists-panel" aria-label={t("language.title")}>
      <div className="profile-language-card" role="list" aria-label={t("language.title")}>
        {languageRows.map((row, index) => (
          <button
            key={row.value}
            type="button"
            role="listitem"
            className={`profile-language-row${index < languageRows.length - 1 ? " profile-language-row-divider" : ""}`}
            onClick={() => setLocalePreference(row.value)}
            aria-label={row.label}
          >
            <span className="profile-language-row-copy">
              <span>{row.label}</span>
              {row.description ? <small className="profile-list-row-subtext">{row.description}</small> : null}
            </span>
            {localePreference === row.value ? <UiIcon name="check-circle-filled" className="ui-icon" /> : null}
          </button>
        ))}
      </div>
    </section>
  );
}

