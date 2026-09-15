interface CodeEditorProps {
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
}

export function CodeEditor({ value, onChange, disabled }: CodeEditorProps) {
  return (
    <textarea
      className="code-editor"
      value={value}
      onChange={(event) => onChange(event.target.value)}
      disabled={disabled}
      spellCheck={false}
      autoCapitalize="off"
      autoCorrect="off"
      aria-label="Java source code"
    />
  );
}
