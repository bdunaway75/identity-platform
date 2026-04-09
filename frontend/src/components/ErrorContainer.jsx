export default function ErrorContainer({errors, className = ""}) {
    const combinedClassName = ["error-container", className].filter(Boolean).join(" ");
    return(
        <div className={combinedClassName}>
            {errors}
        </div>
    )
}
