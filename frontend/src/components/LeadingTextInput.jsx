import "./CustomTextInput.css"

export default function LeadingTextInput({
    prefix,
    placeholder,
    label,
    value,
    onChange,
    error,
}) {
    const inputId = `input_${label.replace(/\s+/g, '_').toLowerCase()}`;

    return (
        <div className="text-input">
            <label className="text-input-label" htmlFor={inputId}>{label}</label>
            <span className="leading-text-prefix">
                <div className={'leading-text-field'}>{prefix}</div>
                <input
                    id={inputId}
                    className="text-input-field"
                    type="text"
                    placeholder={placeholder}
                    value={value}
                    aria-invalid={Boolean(error)}
                    aria-describedby={error ? `${inputId}_error` : undefined}
                    onChange={event => onChange(event.target.value)}
                />
            </span>
            {error ? <div className="text-input-error" id={`${inputId}_error`}>{error}</div> : null}
        </div>
    );
}
