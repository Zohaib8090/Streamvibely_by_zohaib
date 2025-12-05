
import React, { useState } from 'react';
import { View, Text, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';
import LibraryModeModal from '@/components/LibraryModeModal';
import { useRouter } from 'expo-router';

const LibraryScreen = () => {
  const [modalVisible, setModalVisible] = useState(false);
  const [libraryMode, setLibraryMode] = useState('Library');
  const router = useRouter();

  const handleSelectMode = (mode) => {
    if (mode === 'Downloads') {
      router.push('/(drawer)/downloads');
    } else {
      setLibraryMode(mode);
    }
    setModalVisible(false);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity style={styles.libraryTitleContainer} onPress={() => setModalVisible(true)}>
          <Text style={styles.libraryTitle}>{libraryMode}</Text>
          <Ionicons name="chevron-down" size={24} color={Colors.dark.text} />
        </TouchableOpacity>
        <View style={styles.headerIcons}>
          <TouchableOpacity onPress={() => router.push('/(drawer)/history')}>
            <Ionicons name="time-outline" size={24} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity>
            <Ionicons name="search-outline" size={24} color={Colors.dark.text} />
          </TouchableOpacity>
          <TouchableOpacity>
            <View style={styles.avatar}>
              {/* Add user avatar image here */}
            </View>
          </TouchableOpacity>
        </View>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filtersContainer}>
        {['Playlists', 'Songs', 'Albums', 'Artists', 'Profiles', 'Podcasts'].map(filter => (
          <TouchableOpacity key={filter} style={styles.filterChip}>
            <Text style={styles.filterText}>{filter}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      <View style={styles.mainContent}>
        <View style={styles.recentActivityHeader}>
          <Text style={styles.recentActivityTitle}>Recent activity</Text>
          <View style={styles.viewToggle}>
            <Ionicons name="grid" size={20} color={Colors.dark.text} />
          </View>
        </View>
        {/* Add list of library items here */}
      </View>

      <TouchableOpacity style={styles.fab}>
        <Ionicons name="add" size={30} color={Colors.dark.background} />
        <Text style={styles.fabText}>New</Text>
      </TouchableOpacity>

      <LibraryModeModal
        visible={modalVisible}
        onClose={() => setModalVisible(false)}
        onSelect={handleSelectMode}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.dark.background,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 16,
  },
  libraryTitleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  libraryTitle: {
    color: Colors.dark.text,
    fontSize: 24,
    fontFamily: Fonts.bold,
    marginRight: 8,
  },
  headerIcons: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#ccc', // Placeholder
  },
  filtersContainer: {
    paddingHorizontal: 16,
    marginTop: 16,
  },
  filterChip: {
    backgroundColor: '#2C2C2E',
    borderRadius: 8,
    paddingVertical: 6,
    paddingHorizontal: 12,
    marginRight: 8,
  },
  filterText: {
    color: Colors.dark.text,
    fontFamily: Fonts.regular,
  },
  mainContent: {
    flex: 1,
    paddingHorizontal: 16,
    marginTop: 16,
  },
  recentActivityHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  recentActivityTitle: {
    color: Colors.dark.text,
    fontFamily: Fonts.regular,
  },
  viewToggle: {
    // Style for the grid/list view toggle
  },
  fab: {
    position: 'absolute',
    right: 30,
    bottom: 30,
    backgroundColor: 'white',
    borderRadius: 30,
    width: 90,
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
    flexDirection: 'row',
    gap: 5
  },
  fabText: {
    color: 'black',
    fontWeight: 'bold'
  }
});

export default LibraryScreen;
