import {useState} from 'react';
import './App.css';
import {useApi} from './hooks/useApi.tsx';
import {useLoad} from './hooks/useLoad.tsx';
import {Home} from './pages/Home.tsx';
import {Login} from './pages/Login.tsx';

interface LoggedIn {
    success: boolean;
}

function App() {

    const [loggedIn, setLoggedIn] = useState(false);

    const {execute: isLoggedIn} = useApi<LoggedIn>({
        onSuccess: response => setLoggedIn(response.success),
        onError: error => error.status === 401 && setLoggedIn(false),
    });

    useLoad(() => {
        void isLoggedIn(`/login`);
    })

    function handleAuthenticate(authenticated: boolean) {
        return setLoggedIn(authenticated);
    }

    return <>
        {loggedIn ? <Home onAuthenticate={handleAuthenticate} /> : <Login onAuthenticate={handleAuthenticate} />}
    </>

}

export default App;
