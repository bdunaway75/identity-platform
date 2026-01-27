import "./Login.css";
import "./Errors.css";
import { useState } from "react";

import LoginButton from "../components/LoginButton";
import ErrorContainer from "../components/ErrorContainer";

export default function Login() {
  const [errorMsg, setErrorMsg] = useState("");

  return (
    <div className="login-root">
      <div>
        {errorMsg && <ErrorContainer errors={errorMsg}/>}
         <div className="login-card" style={{marginTop: "0px"}}>
        <h1>Please sign in.</h1>
        <LoginButton
          onError={() => setErrorMsg("Unable to authenticate.")}
        />

        <div className="login-footer">
          Authorized users only
        </div>
      </div>
      </div>
    </div>
  );
}

