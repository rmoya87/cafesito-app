import type { ReactNode } from "react";

export function AppOverlayLayers({
  authPrompt,
  barcodeScanner,
  commentSheet,
  createPostSheet,
  notificationsSheet,
  diarySheets
}: {
  authPrompt: ReactNode;
  barcodeScanner: ReactNode;
  commentSheet: ReactNode;
  createPostSheet: ReactNode;
  notificationsSheet: ReactNode;
  diarySheets: ReactNode;
}) {
  return (
    <>
      {authPrompt}
      {barcodeScanner}
      {commentSheet}
      {createPostSheet}
      {notificationsSheet}
      {diarySheets}
    </>
  );
}
