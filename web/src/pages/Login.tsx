import * as React from 'react';
import {useApi} from '../hooks/useApi.tsx';

interface LoginProps {
    onAuthenticate: (authenticated: boolean) => void;
}

interface LoginResponse {
    success: boolean;
    message: string;
}

export function Login({onAuthenticate}: LoginProps) {

    const [meldcode, setMeldcode] = React.useState('804661');
    const [pincode, setPincode] = React.useState('1765');

    const {execute: login} = useApi<LoginResponse>({
        onSuccess: response => onAuthenticate(response.success),
        onError: error => error.status === 401 && onAuthenticate(false),
    });

    function handleLogin(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        void login(`/login`, {
            method: 'POST',
            body: {
                username: meldcode,
                password: pincode
            }
        });
    }

    return <>
        <h1>Login</h1>
        <form action={`/login`} method={`post`} onSubmit={handleLogin}>
            <input name={`meldcode`}
                   type={`text`}
                   value={meldcode}
                   onChange={e => setMeldcode(e.target.value)}
                   placeholder={`Meldcode`}
                   inputMode={`numeric`}/>
            <input name={`pincode`}
                   type={`password`}
                   value={pincode}
                   onChange={e => setPincode(e.target.value)}
                   placeholder={`Pincode`}
                   inputMode={`numeric`}/>
            <button type={`submit`}>Login</button>
        </form>
    </>;
}