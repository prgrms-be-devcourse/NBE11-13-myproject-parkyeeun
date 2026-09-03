import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  fetchTilDocument,
  TilApiError,
  updateTilDocument,
} from "../api/tilApi";
import Layout from "../components/Layout";
import type { TilDocument } from "../types";

type TilEditorPageProps = {
  connectedRepositoryId: number;
  tilDocumentId: number;
};

type CopyStatus = {
  message: string;
  error: boolean;
};

const formatDateTime = (value: string) =>
  new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Seoul",
  }).format(new Date(value));

const isSafeLink = (href: string) =>
  /^(https?:\/\/|mailto:)/i.test(href);

const renderInlineMarkdown = (
  value: string,
  keyPrefix: string,
): ReactNode[] => {
  const nodes: ReactNode[] = [];
  const tokenPattern = /(\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g;
  let cursor = 0;
  let match: RegExpExecArray | null;

  while ((match = tokenPattern.exec(value)) !== null) {
    if (match.index > cursor) {
      nodes.push(value.slice(cursor, match.index));
    }

    const token = match[0];
    const key = `${keyPrefix}-${match.index}`;

    if (token.startsWith("**")) {
      nodes.push(
        <strong key={key} className="font-semibold text-slate-900">
          {token.slice(2, -2)}
        </strong>,
      );
    } else {
      const linkMatch = token.match(/^\[([^\]]+)\]\(([^)]+)\)$/);

      if (linkMatch && isSafeLink(linkMatch[2])) {
        nodes.push(
          <a
            key={key}
            href={linkMatch[2]}
            target="_blank"
            rel="noreferrer"
            className="text-blue-700 underline underline-offset-2"
          >
            {linkMatch[1]}
          </a>,
        );
      } else {
        nodes.push(token);
      }
    }

    cursor = match.index + token.length;
  }

  if (cursor < value.length) {
    nodes.push(value.slice(cursor));
  }

  return nodes;
};

const isBlockStart = (line: string) =>
  /^#{1,2}\s+/.test(line) ||
  /^[-*]\s+/.test(line) ||
  /^>\s?/.test(line);

const renderParagraphLines = (
  lines: string[],
  keyPrefix: string,
) =>
  lines.flatMap((line, index) => [
    ...(index > 0 ? [<br key={`${keyPrefix}-br-${index}`} />] : []),
    ...renderInlineMarkdown(line, `${keyPrefix}-${index}`),
  ]);

const MarkdownPreview = ({ content }: { content: string }) => {
  if (!content.trim()) {
    return (
      <p className="text-sm text-slate-400">
        작성된 Markdown 내용이 없습니다.
      </p>
    );
  }

  const lines = content.replace(/\r\n/g, "\n").split("\n");
  const blocks: ReactNode[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];

    if (!line.trim()) {
      index += 1;
      continue;
    }

    if (line.startsWith("## ")) {
      blocks.push(
        <h2
          key={`h2-${index}`}
          className="mt-6 text-xl font-semibold text-slate-900 first:mt-0"
        >
          {renderInlineMarkdown(line.slice(3), `h2-${index}`)}
        </h2>,
      );
      index += 1;
      continue;
    }

    if (line.startsWith("# ")) {
      blocks.push(
        <h1
          key={`h1-${index}`}
          className="mt-6 text-2xl font-bold text-slate-900 first:mt-0"
        >
          {renderInlineMarkdown(line.slice(2), `h1-${index}`)}
        </h1>,
      );
      index += 1;
      continue;
    }

    if (/^[-*]\s+/.test(line)) {
      const startIndex = index;
      const items: string[] = [];

      while (index < lines.length && /^[-*]\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^[-*]\s+/, ""));
        index += 1;
      }

      blocks.push(
        <ul
          key={`list-${startIndex}`}
          className="mt-4 list-disc space-y-1 pl-6 text-sm leading-7 text-slate-700 first:mt-0"
        >
          {items.map((item, itemIndex) => (
            <li key={`${startIndex}-${itemIndex}`}>
              {renderInlineMarkdown(
                item,
                `list-${startIndex}-${itemIndex}`,
              )}
            </li>
          ))}
        </ul>,
      );
      continue;
    }

    if (/^>\s?/.test(line)) {
      const startIndex = index;
      const quoteLines: string[] = [];

      while (index < lines.length && /^>\s?/.test(lines[index])) {
        quoteLines.push(lines[index].replace(/^>\s?/, ""));
        index += 1;
      }

      blocks.push(
        <blockquote
          key={`quote-${startIndex}`}
          className="mt-4 border-l-4 border-slate-300 pl-4 text-sm leading-7 text-slate-600 first:mt-0"
        >
          {renderParagraphLines(quoteLines, `quote-${startIndex}`)}
        </blockquote>,
      );
      continue;
    }

    const startIndex = index;
    const paragraphLines: string[] = [];

    while (
      index < lines.length &&
      lines[index].trim() &&
      !isBlockStart(lines[index])
    ) {
      paragraphLines.push(lines[index]);
      index += 1;
    }

    blocks.push(
      <p
        key={`paragraph-${startIndex}`}
        className="mt-4 text-sm leading-7 text-slate-700 first:mt-0"
      >
        {renderParagraphLines(
          paragraphLines,
          `paragraph-${startIndex}`,
        )}
      </p>,
    );
  }

  return <>{blocks}</>;
};

function TilEditorPage({
  connectedRepositoryId,
  tilDocumentId,
}: TilEditorPageProps) {
  const [tilDocument, setTilDocument] =
    useState<TilDocument | null>(null);
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [saveMessage, setSaveMessage] = useState("");
  const [saving, setSaving] = useState(false);
  const [copyStatus, setCopyStatus] =
    useState<CopyStatus | null>(null);

  useEffect(() => {
    let cancelled = false;

    const loadTilDocument = async () => {
      setLoading(true);
      setNotFound(false);
      setLoadError("");

      try {
        const document = await fetchTilDocument(
          connectedRepositoryId,
          tilDocumentId,
        );

        if (!cancelled) {
          setTilDocument(document);
          setContent(document.content ?? "");
        }
      } catch (error) {
        if (!cancelled) {
          if (error instanceof TilApiError && error.status === 404) {
            setNotFound(true);
          } else {
            setLoadError(
              error instanceof Error
                ? error.message
                : "TIL 문서를 불러오는 중 오류가 발생했습니다.",
            );
          }
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void loadTilDocument();

    return () => {
      cancelled = true;
    };
  }, [connectedRepositoryId, tilDocumentId]);

  const hasChanges =
    tilDocument !== null && content !== tilDocument.content;
  const contentIsEmpty = !content.trim();

  const handleContentChange = (value: string) => {
    setContent(value);
    setSaveError("");
    setSaveMessage("");
    setCopyStatus(null);
  };

  const handleSave = async () => {
    if (!hasChanges || contentIsEmpty) {
      return;
    }

    setSaving(true);
    setSaveError("");
    setSaveMessage("");

    try {
      const updatedDocument = await updateTilDocument(
        connectedRepositoryId,
        tilDocumentId,
        content,
      );

      setTilDocument(updatedDocument);
      setContent(updatedDocument.content);
      setSaveMessage("저장했습니다.");
    } catch (error) {
      setSaveError(
        error instanceof Error
          ? error.message
          : "TIL 저장 중 오류가 발생했습니다.",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCopy = async () => {
    setCopyStatus(null);

    if (contentIsEmpty) {
      setCopyStatus({
        message: "복사할 Markdown 내용이 없습니다.",
        error: true,
      });
      return;
    }

    try {
      await navigator.clipboard.writeText(content);
      setCopyStatus({ message: "복사했습니다.", error: false });
    } catch {
      setCopyStatus({
        message: "Markdown 복사에 실패했습니다.",
        error: true,
      });
    }
  };

  const moveToAnalysis = () => {
    window.location.assign(
      `/repositories/${connectedRepositoryId}/analysis`,
    );
  };

  if (loading) {
    return (
      <Layout>
        <p className="text-sm text-slate-500">
          TIL 문서를 불러오는 중입니다.
        </p>
      </Layout>
    );
  }

  if (notFound || loadError || !tilDocument) {
    return (
      <Layout>
        <div className="rounded-xl border border-slate-200 px-5 py-8 text-center">
          <h2 className="text-lg font-semibold text-slate-900">
            {notFound
              ? "TIL 문서를 찾을 수 없습니다."
              : "TIL 문서를 불러오지 못했습니다."}
          </h2>

          {loadError && (
            <p className="mt-2 text-sm text-red-700">{loadError}</p>
          )}

          <button
            type="button"
            onClick={moveToAnalysis}
            className="mt-5 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
          >
            ← 분석 화면으로 돌아가기
          </button>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <button
            type="button"
            onClick={moveToAnalysis}
            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
          >
            ← 분석 화면으로 돌아가기
          </button>

          <h2 className="mt-3 text-xl font-semibold text-slate-900">
            {tilDocument.title}
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            마지막 저장: {formatDateTime(tilDocument.updatedAt)}
            {hasChanges && " · 저장하지 않은 변경 사항이 있습니다."}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => void handleCopy()}
            className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
          >
            Markdown 복사
          </button>

          <button
            type="button"
            onClick={() => void handleSave()}
            disabled={!hasChanges || contentIsEmpty || saving}
            className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>

      {(saveError || saveMessage || copyStatus) && (
        <div
          className={`mt-5 rounded-lg border px-4 py-3 text-sm ${
            saveError || copyStatus?.error
              ? "border-red-200 bg-red-50 text-red-700"
              : "border-emerald-200 bg-emerald-50 text-emerald-700"
          }`}
        >
          {saveError || copyStatus?.message || saveMessage}
        </div>
      )}

      {contentIsEmpty && (
        <p className="mt-5 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
          Markdown 내용이 비어 있습니다. 내용을 입력해야 저장할 수
          있습니다.
        </p>
      )}

      <div className="mt-6 grid gap-5 lg:grid-cols-2">
        <section>
          <label
            htmlFor="til-markdown"
            className="text-sm font-semibold text-slate-900"
          >
            Markdown 편집
          </label>

          <textarea
            id="til-markdown"
            value={content}
            onChange={(event) =>
              handleContentChange(event.target.value)
            }
            spellCheck={false}
            className="mt-3 h-[clamp(32rem,65vh,40rem)] w-full resize-y rounded-xl border border-slate-200 bg-white p-4 font-mono text-sm leading-6 text-slate-900 outline-none focus:border-slate-400"
            placeholder="TIL Markdown 내용을 입력하세요."
          />
        </section>

        <section>
          <h3 className="text-sm font-semibold text-slate-900">
            미리보기
          </h3>

          <div className="mt-3 h-[clamp(32rem,65vh,40rem)] overflow-y-auto rounded-xl border border-slate-200 bg-slate-50/60 p-5">
            <MarkdownPreview content={content} />
          </div>
        </section>
      </div>
    </Layout>
  );
}

export default TilEditorPage;
