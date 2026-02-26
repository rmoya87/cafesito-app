import { useCallback } from "react";

type GuardOptions = {
  sessionEmail: string | null;
  onRequireAuth: () => void;
};

export function useAuthActionGuard({ sessionEmail, onRequireAuth }: GuardOptions) {
  const canUseProtectedActions = Boolean(sessionEmail);

  const runWithAuth = useCallback(
    async (action: () => void | Promise<void>) => {
      if (!canUseProtectedActions) {
        onRequireAuth();
        return;
      }
      await action();
    },
    [canUseProtectedActions, onRequireAuth]
  );

  return { canUseProtectedActions, runWithAuth };
}
