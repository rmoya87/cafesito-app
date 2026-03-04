import { Button } from "./components";

type MentionVisualUser = {
  username: string;
  avatarUrl?: string | null;
};

function MentionInlineChip({
  username,
  visualUser,
  onClick
}: {
  username: string;
  visualUser: MentionVisualUser | null | undefined;
  onClick?: (username: string) => void;
}) {
  const hasAvatar = Boolean(visualUser?.avatarUrl);
  return (
    <Button key={username} variant="plain" type="button" className="mention-button" onClick={() => onClick?.(username)}>
      {hasAvatar ? (
        <img
          className="mention-button-avatar"
          src={visualUser?.avatarUrl ?? ""}
          alt={visualUser?.username ?? username}
          loading="lazy"
          decoding="async"
          referrerPolicy="no-referrer"
          crossOrigin="anonymous"
          onError={(event) => {
            const target = event.currentTarget;
            target.style.display = "none";
          }}
        />
      ) : (
        <span className="mention-button-avatar-fallback" aria-hidden="true">
          {(visualUser?.username ?? username).slice(0, 1).toUpperCase()}
        </span>
      )}
      <span>@{username}</span>
    </Button>
  );
}

export function MentionText({
  text,
  onMentionClick,
  resolveMentionUser
}: {
  text: string;
  onMentionClick?: (username: string) => void;
  resolveMentionUser?: (username: string) => MentionVisualUser | null | undefined;
}) {
  const mentionRegex = /@([A-Za-z0-9._-]{2,30})/g;
  const parts: Array<{ value: string; mention: boolean; key: string }> = [];
  let lastIndex = 0;
  let matchIndex = 0;

  for (const match of text.matchAll(mentionRegex)) {
    const index = match.index ?? 0;
    if (index > lastIndex) {
      const chunk = text.slice(lastIndex, index);
      parts.push({ value: chunk, mention: false, key: `t-${matchIndex}-${index}` });
    }
    const username = match[1] ?? "";
    parts.push({ value: `@${username}`, mention: true, key: `m-${matchIndex}-${username}` });
    lastIndex = index + match[0].length;
    matchIndex += 1;
  }

  if (lastIndex < text.length) {
    parts.push({ value: text.slice(lastIndex), mention: false, key: `t-end-${lastIndex}` });
  }

  if (!parts.length) return <>{text}</>;

  return (
    <>
      {parts.map((part) =>
        part.mention ? (
          <MentionInlineChip
            key={part.key}
            username={part.value.slice(1)}
            visualUser={resolveMentionUser?.(part.value.slice(1))}
            onClick={onMentionClick}
          />
        ) : (
          <span key={part.key}>{part.value}</span>
        )
      )}
    </>
  );
}
