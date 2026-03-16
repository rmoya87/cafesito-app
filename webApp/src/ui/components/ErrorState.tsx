/**
 * Estado de error unificado (mensaje + Reintentar).
 * Ver docs/UX_EMPTY_AND_ERROR_STATES.md.
 */

import { ERROR } from "../../core/emptyErrorStrings";
import { Button } from "./Button";

type ErrorStateProps = {
  message?: string;
  detail?: string;
  onRetry: () => void;
  className?: string;
};

export function ErrorState({
  message = ERROR.LOAD_DATA,
  detail,
  onRetry,
  className = "",
}: ErrorStateProps) {
  return (
    <article
      className={`home-empty home-error ui-error-state ${className}`.trim()}
      role="alert"
    >
      <h3>{message}</h3>
      {detail ? <p>{detail}</p> : null}
      <Button
        variant="ghost"
        type="button"
        className="action-button"
        onClick={() => void onRetry()}
        aria-label={`${ERROR.RETRY} carga`}
      >
        {ERROR.RETRY}
      </Button>
    </article>
  );
}
