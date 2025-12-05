
import React from 'react';
import { View, Text, StyleSheet, Modal, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '@/constants/Colors';
import { Fonts } from '@/constants/Fonts';

const LibraryModeModal = ({ visible, onClose, onSelect }) => {
  return (
    <Modal
      animationType="slide"
      transparent={true}
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.modalContainer}>
        <View style={styles.modalContent}>
          <TouchableOpacity style={styles.closeButton} onPress={onClose}>
            <Ionicons name="close" size={30} color={Colors.dark.text} />
          </TouchableOpacity>
          <Text style={styles.title}>View my</Text>
          <TouchableOpacity style={styles.option} onPress={() => onSelect('Library')}>
            <Ionicons name="checkmark" size={24} color={Colors.dark.text} />
            <Text style={styles.optionText}>Library</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.option} onPress={() => onSelect('Downloads')}>
            <Text style={styles.optionText}>Downloads</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalContainer: {
    flex: 1,
    justifyContent: 'flex-end',
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  modalContent: {
    backgroundColor: Colors.dark.background,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
  },
  closeButton: {
    alignSelf: 'flex-end',
  },
  title: {
    color: Colors.dark.text,
    fontSize: 24,
    fontFamily: Fonts.bold,
    marginBottom: 20,
  },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 15,
  },
  optionText: {
    color: Colors.dark.text,
    fontSize: 18,
    fontFamily: Fonts.regular,
    marginLeft: 15,
  },
});

export default LibraryModeModal;
