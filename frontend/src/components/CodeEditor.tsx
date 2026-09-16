import { useLayoutEffect, useRef } from "react";
import type { KeyboardEvent } from "react";

interface CodeEditorProps {
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
}

const INDENT = "    ";

function hasNoModifiers(event: KeyboardEvent<HTMLTextAreaElement>): boolean {
  return !event.shiftKey && !event.ctrlKey && !event.metaKey && !event.altKey;
}

function currentLineIndentation(text: string, caretPosition: number): string {
  const lineStart = text.lastIndexOf("\n", caretPosition - 1) + 1;
  const line = text.slice(lineStart, caretPosition);
  return /^[ \t]*/.exec(line)?.[0] ?? "";
}

function endsWithOpenBrace(text: string, caretPosition: number): boolean {
  return /\{[ \t]*$/.test(text.slice(0, caretPosition));
}

export function CodeEditor({ value, onChange, disabled }: CodeEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const pendingCaretRef = useRef<number | null>(null);

  // Restoring the caret here (rather than in an onChange/useEffect) runs
  // synchronously before the browser paints, so there is no visible jump to
  // the end of the text between the keypress and the caret landing back in
  // the right place.
  useLayoutEffect(() => {
    const caret = pendingCaretRef.current;
    if (caret !== null && textareaRef.current) {
      textareaRef.current.setSelectionRange(caret, caret);
      pendingCaretRef.current = null;
    }
  }, [value]);

  function replaceSelection(insertion: string, selectionStart: number, selectionEnd: number) {
    const newValue = value.slice(0, selectionStart) + insertion + value.slice(selectionEnd);
    pendingCaretRef.current = selectionStart + insertion.length;
    onChange(newValue);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (!hasNoModifiers(event)) return;

    const { selectionStart, selectionEnd } = event.currentTarget;

    if (event.key === "Tab") {
      event.preventDefault();
      replaceSelection(INDENT, selectionStart, selectionEnd);
      return;
    }

    if (event.key === "Enter") {
      event.preventDefault();
      const indentation = currentLineIndentation(value, selectionStart);
      const extra = endsWithOpenBrace(value, selectionStart) ? INDENT : "";
      replaceSelection("\n" + indentation + extra, selectionStart, selectionEnd);
    }
  }

  return (
    <textarea
      ref={textareaRef}
      className="code-editor"
      value={value}
      onChange={(event) => onChange(event.target.value)}
      onKeyDown={handleKeyDown}
      disabled={disabled}
      spellCheck={false}
      autoCapitalize="off"
      autoCorrect="off"
      aria-label="Java source code"
    />
  );
}
