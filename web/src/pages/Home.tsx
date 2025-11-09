import {useState} from 'react';
import {useApi} from '../hooks/useApi.tsx';
import {useLoad} from '../hooks/useLoad.tsx';
import type {Parking} from '../types/Parking.tsx';
import type {User} from '../types/User.tsx';
import type {Visitor} from '../types/Visitor.tsx';

interface HomeProps {
    onAuthenticate: (authenticated: boolean) => void;
}

interface VisitorResponse {
    visitors: Visitor[];
}

interface ParkingResponse {
    active: Parking[];
    scheduled: Parking[];
}

export function Home({onAuthenticate}: HomeProps) {

    const [user, setUser] = useState<User | null>(null);
    const [visitorList, setVisitorList] = useState<Visitor[]>([]);
    const [activeParkingList, setActiveParkingList] = useState<Parking[]>([]);
    const [scheduledParkingList, setScheduledParkingList] = useState<Parking[]>([]);

    const {execute: getUser} = useApi<User>({
        onSuccess: setUser,
        onError: error => error.status === 401 && onAuthenticate(false),
    });

    const {execute: getVisitors} = useApi<VisitorResponse>({
        onSuccess: response => setVisitorList(response.visitors),
        onError: error => error.status === 401 && onAuthenticate(false),
    });

    const {execute: getParking} = useApi<ParkingResponse>({
        onSuccess: response => {
            setActiveParkingList(response.active);
            setScheduledParkingList(response.scheduled);
        },
        onError: error => error.status === 401 && onAuthenticate(false),
    });

    const {execute: logout} = useApi<never>({
        onSuccess: () => onAuthenticate(false),
        onError: error => error.status === 401 && onAuthenticate(false),
    });

    useLoad(() => {
        void getUser(`/user`);
        void getVisitors(`/visitor`);
        void getParking(`/parking`);
    });

    function handleLogout() {
        void logout(`/logout`);
    }

    return <>
        <button onClick={handleLogout}>Logout</button>
        <div>{user?.balance ?? ''}</div>
        <h3>Active Parking</h3>
        <ul>
            {activeParkingList.map((parking) => (
                <li key={parking.id}>{parking.name ?? parking.license}</li>
            ))}
        </ul>
        <h3>Scheduled Parking</h3>
        <ul>
            {scheduledParkingList.map((parking) => (
                <li key={parking.id}>{parking.name ?? parking.license}</li>
            ))}
        </ul>
        <h3>Visitors</h3>
        <ul>
            {visitorList.map((visitor) => (
                <li key={visitor.visitorId}>{visitor.name ?? visitor.formattedLicense}</li>
            ))}
        </ul>
    </>;

}