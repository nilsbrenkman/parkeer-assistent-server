import {useEffect, useRef} from 'react';

export function useLoad(callback: () => void) {

    const loadRef = useRef(false);

    useEffect(() => {
        if (loadRef.current) return;
        loadRef.current = true;
        callback();
    }, [callback]);

}
