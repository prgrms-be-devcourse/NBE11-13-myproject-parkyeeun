import { useState } from "react";
import type { StoredFileAnalysis } from "../../types/analysis";

type FileGroupListProps = {
  files: StoredFileAnalysis[];
};

type FileGroup = {
  name: string;
  files: StoredFileAnalysis[];
};

const getPathParts = (path: string) => {
  return path.split("/").filter(Boolean);
};

const getCommonDirectoryParts = (
  files: StoredFileAnalysis[],
) => {
  if (files.length === 0) {
    return [];
  }

  const directoryParts = files.map((file) => {
    const parts = getPathParts(file.filename);

    return parts.slice(0, -1);
  });

  const shortestLength = Math.min(
    ...directoryParts.map((parts) => parts.length),
  );

  const commonParts: string[] = [];

  for (let index = 0; index < shortestLength; index += 1) {
    const currentPart = directoryParts[0][index];

    const everyPathMatches = directoryParts.every(
      (parts) => parts[index] === currentPart,
    );

    if (!everyPathMatches) {
      break;
    }

    commonParts.push(currentPart);
  }

  return commonParts;
};

const getSharedValue = (
  values: Array<string | null>,
) => {
  if (values.length === 0) {
    return null;
  }

  const firstValue = values[0];

  return values.every((value) => value === firstValue)
    ? firstValue
    : null;
};

const groupFiles = (
  files: StoredFileAnalysis[],
  commonDirectoryParts: string[],
): FileGroup[] => {
  const groups = new Map<string, StoredFileAnalysis[]>();

  files.forEach((file) => {
    const parts = getPathParts(file.filename);
    const relativeParts = parts.slice(
      commonDirectoryParts.length,
    );

    const groupName =
      relativeParts.length > 1 ? relativeParts[0] : "기타";

    const groupFiles = groups.get(groupName) ?? [];

    groupFiles.push(file);
    groups.set(groupName, groupFiles);
  });

  return Array.from(groups.entries())
    .map(([name, groupedFiles]) => ({
      name,
      files: groupedFiles,
    }))
    .sort((first, second) => {
      if (first.name === "기타") {
        return 1;
      }

      if (second.name === "기타") {
        return -1;
      }

      return first.name.localeCompare(second.name);
    });
};

function FileGroupList({ files }: FileGroupListProps) {
  const [expandedGroups, setExpandedGroups] = useState<
    Set<string>
  >(new Set());

  const commonDirectoryParts =
    getCommonDirectoryParts(files);
  const commonDirectoryPath =
    commonDirectoryParts.join("/");

  const sharedStatus = getSharedValue(
    files.map((file) => file.status),
  );
  const sharedCategory = getSharedValue(
    files.map((file) => file.category),
  );
  const sharedScope = getSharedValue(
    files.map((file) => file.scope),
  );

  const groups = groupFiles(
    files,
    commonDirectoryParts,
  );

  const toggleGroup = (groupName: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev);

      if (next.has(groupName)) {
        next.delete(groupName);
      } else {
        next.add(groupName);
      }

      return next;
    });
  };

  const getRelativeFilePath = (
    file: StoredFileAnalysis,
    groupName: string,
  ) => {
    const parts = getPathParts(file.filename);
    const relativeParts = parts.slice(
      commonDirectoryParts.length,
    );

    if (
      groupName !== "기타" &&
      relativeParts[0] === groupName
    ) {
      return relativeParts.slice(1).join("/");
    }

    return relativeParts.join("/");
  };

  return (
    <div className="mt-3 overflow-hidden rounded-xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 bg-slate-50 px-4 py-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm font-medium text-slate-900">
              변경 파일 {files.length}개
            </p>

            {commonDirectoryPath && (
              <p className="mt-1 break-all text-xs text-slate-500">
                {commonDirectoryPath}/
              </p>
            )}
          </div>

          <div className="flex flex-wrap gap-2">
            {sharedStatus && (
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600">
                {sharedStatus}
              </span>
            )}

            {sharedCategory && (
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600">
                category: {sharedCategory}
              </span>
            )}

            {sharedScope && (
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600">
                scope: {sharedScope}
              </span>
            )}
          </div>
        </div>
      </div>

      <ul className="divide-y divide-slate-100">
        {groups.map((group) => {
          const expanded = expandedGroups.has(group.name);

          return (
            <li key={group.name}>
              <button
                type="button"
                onClick={() => toggleGroup(group.name)}
                className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left transition-colors hover:bg-slate-50"
              >
                <div className="min-w-0">
                  <p className="font-medium text-slate-900">
                    {group.name === "기타"
                      ? "기타 파일"
                      : `${group.name}/`}
                  </p>

                  <p className="mt-1 text-xs text-slate-500">
                    {group.files.length}개 파일
                  </p>
                </div>

                <span className="shrink-0 text-sm text-slate-500">
                  {expanded ? "접기" : "보기"}
                </span>
              </button>

              {expanded && (
                <ul className="border-t border-slate-100 bg-slate-50/70">
                  {group.files.map((file, index) => {
                    const relativeFilePath =
                      getRelativeFilePath(
                        file,
                        group.name,
                      );

                    const showStatus =
                      file.status !== sharedStatus;
                    const showCategory =
                      file.category !== sharedCategory;
                    const showScope =
                      file.scope !== sharedScope;

                    return (
                      <li
                        key={`${file.filename}-${index}`}
                        className="border-b border-slate-100 px-4 py-3 last:border-b-0"
                      >
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div className="min-w-0">
                            <p className="break-all text-sm text-slate-900">
                              {relativeFilePath ||
                                file.filename}
                            </p>

                            {file.previousFilename && (
                              <p className="mt-1 break-all text-xs text-slate-500">
                                이전 경로:{" "}
                                {file.previousFilename}
                              </p>
                            )}
                          </div>

                          {(showStatus ||
                            showCategory ||
                            showScope) && (
                            <div className="flex flex-wrap gap-2">
                              {showStatus && (
                                <span className="rounded-full bg-white px-2.5 py-1 text-xs text-slate-600">
                                  {file.status}
                                </span>
                              )}

                              {showCategory &&
                                file.category && (
                                  <span className="rounded-full bg-white px-2.5 py-1 text-xs text-slate-600">
                                    category:{" "}
                                    {file.category}
                                  </span>
                                )}

                              {showScope && file.scope && (
                                <span className="rounded-full bg-white px-2.5 py-1 text-xs text-slate-600">
                                  scope: {file.scope}
                                </span>
                              )}
                            </div>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default FileGroupList;