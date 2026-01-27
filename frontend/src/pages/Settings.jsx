export default function Settings() {
  return (
    <>
      <h2>Settings</h2>

      <form style={{ maxWidth: "400px" }}>
        <label>
          Display name
          <input
            type="text"
            placeholder="My App"
            style={{ width: "100%", marginTop: "0.5rem" }}
          />
        </label>
      </form>
    </>
  );
}
