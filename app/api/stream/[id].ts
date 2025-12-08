
import { ExpoRequest, ExpoResponse } from 'expo-router/server';

// A list of multiple, independent Piped API instances for fallback
const API_INSTANCES = [
  'https://pipedapi.in.projectsegfau.lt',
  'https://piped-api.garudalinux.org',
  'https://pipedapi.tokhmi.xyz',
  'https://pipedapi.kavin.rocks'
];

export async function GET(request: ExpoRequest, { id }: { id: string }) {
  if (!id) {
    return ExpoResponse.json({ error: 'Video ID is required' }, { status: 400 });
  }

  let streamData = null;

  // Iterate through API instances until a successful fetch occurs
  for (const baseUrl of API_INSTANCES) {
    try {
      const response = await fetch(`${baseUrl}/streams/${id}`);
      if (response.ok) {
        streamData = await response.json();
        console.log(`Successfully fetched stream data from ${baseUrl}`);
        break; // Exit the loop on a successful fetch
      }
    } catch (error) {
      console.warn(`Failed to fetch from ${baseUrl}. Trying next instance...`);
    }
  }

  if (!streamData) {
    return ExpoResponse.json(
      { error: "Failed to fetch stream data from all available API instances." },
      { status: 500 }
    );
  }

  // Find a suitable audio stream
  const audioStream = streamData.audioStreams.find(s => s.itag === 140 && s.mimeType === "audio/mp4");

  if (!audioStream || !audioStream.url) {
    return ExpoResponse.json(
      { error: "Could not find a suitable audio stream URL in the fetched data." },
      { status: 404 }
    );
  }

  // Return the found audio stream URL
  return ExpoResponse.json({ url: audioStream.url });
}
