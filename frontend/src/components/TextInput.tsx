import { useId } from "react";
import type { HTMLAttributes, HTMLInputTypeAttribute, ReactNode } from "react";
import "./CustomTextInput.css";

type TextInputProps = {
  label: ReactNode;
  placeholder?: string;
  value: string | number;
  onChange: (value: string) => void;
  error?: string;
  disabled?: boolean;
  type?: HTMLInputTypeAttribute;
  inputMode?: HTMLAttributes<HTMLInputElement>["inputMode"];
  min?: number | string;
  step?: number | string;
};

export default function TextInput({
  label,
  placeholder,
  value,
  onChange,
  error,
  disabled = false,
  type = "text",
  inputMode,
  min,
  step,
}: TextInputProps) {
  const reactId = useId();
  const labelSlug =
    typeof label === "string" ? label.replace(/\s+/g, "_").toLowerCase() : "field";
  const inputId = `input_${labelSlug}_${reactId.replace(/[:]/g, "")}`;

  return (
    <div className="text-input">
      <label className="text-input-label" htmlFor={inputId}>
        {label}
      </label>
      <input
        id={inputId}
        placeholder={placeholder}
        className="text-input-field"
        type={type}
        value={value}
        inputMode={inputMode}
        min={min}
        step={step}
        disabled={disabled}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${inputId}_error` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? (
        <div className="text-input-error" id={`${inputId}_error`}>
          {error}
        </div>
      ) : null}
    </div>
  );
}
