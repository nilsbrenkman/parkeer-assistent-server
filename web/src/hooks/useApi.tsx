import {useCallback, useState} from 'react';
import {type ErrorResponse, fetchWrapper} from '../apiClient';

interface UseApiOptions<T> {
    onSuccess?: (data: T) => void;
    onError?: (error: ErrorResponse) => void;
    onNoContent?: () => void;
    onFinally?: () => void;
}

export function useApi<T>(options: UseApiOptions<T> = {}) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse | null>(null);
    const [data, setData] = useState<T | null>(null);

    const execute = useCallback(async (
        url: string,
        fetchOptions?: Parameters<typeof fetchWrapper>[1],
    ) => {
        setLoading(true);
        setError(null);

        const response = await fetchWrapper<T>(url, fetchOptions);

        if (response.error) {
            setError(response.error);
            options.onError?.(response.error);
        } else if (response.data) {
            setData(response.data);
            options.onSuccess?.(response.data);
        } else if (response.noContent === true) {
            options.onNoContent?.();
        }
        options.onFinally?.();

        setLoading(false);
        return response;
    }, [options]);

    return {execute, loading, error, data};

}
