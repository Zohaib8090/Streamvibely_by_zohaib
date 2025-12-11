
import 'react-native-gesture-handler';
import { Drawer } from 'expo-router/drawer';
import CustomDrawerContent from './drawer';
import { useNotifications } from '@/hooks/useNotifications';
import { usePlayer } from '@/contexts/PlayerContext';
import MiniPlayer from '@/components/MiniPlayer';
import FullPlayer from '@/components/FullPlayer';

// This component encapsulates the UI that depends on the PlayerContext.
const PlayerUI = () => {
    const { playerState } = usePlayer();
    return (
        <>
            {playerState === 'mini' && <MiniPlayer />}
            {playerState === 'full' && <FullPlayer />}
        </>
    );
}

const DrawerLayout = () => {
    useNotifications();

    return (
        <>
            <Drawer
                drawerContent={(props) => <CustomDrawerContent {...props} />}
                screenOptions={({ route }) => ({
                    headerShown: false,
                    drawerActiveTintColor: '#fff',
                    drawerInactiveTintColor: '#fff',
                    drawerItemStyle:
                        route.name === '(tabs)' || route.name === 'drawer'
                            ? { display: 'none' }
                            : {},
                })}
            />
            {/* By rendering PlayerUI here, we ensure usePlayer() is called within a component that is a guaranteed child of the main PlayerProvider. */}
            <PlayerUI />
        </>
    );
};

export default DrawerLayout;
