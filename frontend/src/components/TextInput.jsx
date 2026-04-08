import './CustomTextInput.css'

export default function TextInput({
    label,
    placeholder,
    value,
    onChange,
    error,
    type = 'text',
    inputMode,
    min,
    step,
}) {
    const inputId = `input_${label.replace(/\s+/g, '_').toLowerCase()}`;

    return (
        <div className="text-input">
            <label className="text-input-label" htmlFor={inputId}>{label}</label>
            <input
                id={inputId}
                placeholder={placeholder}
                className="text-input-field"
                type={type}
                value={value}
                inputMode={inputMode}
                min={min}
                step={step}
                aria-invalid={Boolean(error)}
                aria-describedby={error ? `${inputId}_error` : undefined}
                onChange={e => onChange(e.target.value)}
            />
            {error ? <div className="text-input-error" id={`${inputId}_error`}>{error}</div> : null}
        </div>
    );
}
