import { Button } from "./components";

export function MentionText({ text, onMentionClick }: { text: string; onMentionClick?: (username: string) => void }) {
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
          <Button key={part.key} variant="plain" type="button" className="mention-button" onClick={() => onMentionClick?.(part.value.slice(1))}>
            {part.value}
          </Button>
        ) : (
          <span key={part.key}>{part.value}</span>
        )
      )}
    </>
  );
}
