import { useCallback } from "react";

type GuardOptions = {
  sessionEmail: string | null;
  onRequireAuth: () => void;
};

export function useAuthActionGuard({ sessionEmail, onRequireAuth }: GuardOptions) {
  const canUseProtectedActions = Boolean(sessionEmail);

  const runWithAuth = useCallback(
    async <T,>(action: () => T | Promise<T>): Promise<T | null> => {
      if (!canUseProtectedActions) {
        onRequireAuth();
        return null;
      }
      return await action();
    },
    [canUseProtectedActions, onRequireAuth]
  );

  return { canUseProtectedActions, runWithAuth };
}
