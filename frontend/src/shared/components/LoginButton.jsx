import { useState } from "react";
import { userManager } from "../../features/auth/services/oidc";
import Spinner from 'react-bootstrap/Spinner';

export default function LoginButton({ onError, disabledLabel = "Login", disabled = false, ...props }) {
  const [redirecting, setRedirecting] = useState(false);

  const handleClick = async () => {
    if (redirecting || disabled) return;

    setRedirecting(true);

    try {
      await userManager.signinRedirect();
    } catch (err) {
      console.error("Redirect failed", err);
      setRedirecting(false);
      onError?.() //JS syntax for exists and is a function, call it
    }
  };

  return (
    <button
      className="login-button"
      onClick={handleClick}
      disabled={disabled || redirecting}
      {...props}
    >
      {redirecting ? <Spinner animation="grow" size="sm" /> : disabled ? disabledLabel : "Login"}
    </button>
  );
}
