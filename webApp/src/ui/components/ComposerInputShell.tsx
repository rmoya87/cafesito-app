import type { ReactNode } from "react";
import { Button } from "./Button";
import { Textarea } from "./Textarea";
import { cn } from "./cn";

export type MentionSuggestionItem = {
  id: string | number;
  username: string;
  avatar_url?: string | null;
};

export function ComposerInputShell({
  value,
  onChange,
  placeholder,
  rows = 3,
  textareaId,
  textareaClassName,
  shellClassName,
  showEmojiPanel = false,
  emojis = [],
  onEmojiSelect,
  mentionSuggestions = [],
  onInsertMention,
  resolveMentionUser,
  emojiPanelClassName,
  mentionPanelClassName,
  toolsContent,
  actionContent,
  bottomClassName,
  toolsWrapClassName,
  extraContent
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  textareaId?: string;
  textareaClassName?: string;
  shellClassName?: string;
  showEmojiPanel?: boolean;
  emojis?: string[];
  onEmojiSelect?: (emoji: string) => void;
  mentionSuggestions?: MentionSuggestionItem[];
  onInsertMention?: (username: string) => void;
  resolveMentionUser?: (username: string) => { username: string; avatarUrl?: string | null } | null | undefined;
  emojiPanelClassName?: string;
  mentionPanelClassName?: string;
  toolsContent?: ReactNode;
  actionContent?: ReactNode;
  bottomClassName?: string;
  toolsWrapClassName?: string;
  extraContent?: ReactNode;
}) {
  const mentionRegex = /(^|\s)@([A-Za-z0-9._-]{2,30})(?=\s|$)/g;
  const richParts: Array<{
    type: "text" | "mention";
    value: string;
    key: string;
    resolved?: { username: string; avatarUrl?: string | null } | null;
  }> = [];
  let lastIndex = 0;
  let matchNumber = 0;
  for (const match of value.matchAll(mentionRegex)) {
    const index = match.index ?? 0;
    const leading = match[1] ?? "";
    const username = match[2] ?? "";
    const leadingLen = leading.length;
    const mentionStart = index + leadingLen;
    const mentionEnd = mentionStart + username.length + 1;
    if (mentionStart > lastIndex) {
      richParts.push({ type: "text", value: value.slice(lastIndex, mentionStart), key: `t-${matchNumber}-${lastIndex}` });
    }
    const resolved = resolveMentionUser?.(username) ?? null;
    if (resolved) {
      richParts.push({ type: "mention", value: username, key: `m-${matchNumber}-${username}-${mentionStart}`, resolved });
    } else {
      richParts.push({ type: "text", value: value.slice(mentionStart, mentionEnd), key: `u-${matchNumber}-${mentionStart}` });
    }
    lastIndex = mentionEnd;
    matchNumber += 1;
  }
  if (lastIndex < value.length) {
    richParts.push({ type: "text", value: value.slice(lastIndex), key: `t-end-${lastIndex}` });
  }
  if (!richParts.length) {
    richParts.push({ type: "text", value, key: "t-empty" });
  }

  return (
    <div className={cn("sheet-input-shell composer-input-shell", shellClassName)}>
      {showEmojiPanel ? (
        <div className={cn("comment-inline-panel emoji-panel", emojiPanelClassName)}>
          {emojis.map((emoji) => (
            <Button key={emoji} variant="plain" className="emoji-chip" onClick={() => onEmojiSelect?.(emoji)}>
              {emoji}
            </Button>
          ))}
        </div>
      ) : mentionSuggestions.length ? (
        <div className={cn("comment-inline-panel mention-suggestions", mentionPanelClassName)}>
          {mentionSuggestions.map((user) => (
            <Button
              key={user.id}
              variant="plain"
              className="mention-chip"
              onClick={() => onInsertMention?.(user.username)}
            >
              {user.avatar_url ? (
                <img
                  className="mention-chip-avatar"
                  src={user.avatar_url}
                  alt={user.username}
                  loading="lazy"
                  decoding="async"
                  referrerPolicy="no-referrer"
                  crossOrigin="anonymous"
                />
              ) : (
                <span className="mention-chip-fallback">{user.username.slice(0, 1).toUpperCase()}</span>
              )}
              <span>@{user.username}</span>
            </Button>
          ))}
        </div>
      ) : null}
      <div className="composer-textarea-stack">
        {value ? (
          <div className="composer-text-mirror" aria-hidden="true">
            {richParts.map((part) => {
              if (part.type === "text") return <span key={part.key}>{part.value}</span>;
              const avatar = part.resolved?.avatarUrl ?? null;
              return (
                <span key={part.key} className="mention-button composer-mention-pill">
                  {avatar ? (
                    <img
                      className="mention-button-avatar"
                      src={avatar}
                      alt={part.resolved?.username ?? part.value}
                      loading="lazy"
                      decoding="async"
                      referrerPolicy="no-referrer"
                      crossOrigin="anonymous"
                    />
                  ) : (
                    <span className="mention-button-avatar-fallback" aria-hidden="true">
                      {(part.resolved?.username ?? part.value).slice(0, 1).toUpperCase()}
                    </span>
                  )}
                  <span>@{part.value}</span>
                </span>
              );
            })}
          </div>
        ) : null}
        <Textarea
          id={textareaId}
          className={cn("search-wide sheet-input composer-input-textarea", textareaClassName)}
          placeholder={placeholder}
          rows={rows}
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
      <div className={cn("sheet-composer-bottom", bottomClassName)}>
        <div className={cn("sheet-composer-tools-inline", toolsWrapClassName)}>{toolsContent}</div>
        {actionContent}
      </div>
      {extraContent}
    </div>
  );
}

