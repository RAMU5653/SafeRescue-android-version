let googleMapsLoaderPromise: Promise<void> | null = null;
let isLoaded = false;

export function loadGoogleMaps(apiKey: string): Promise<void> {
  if (isLoaded) return Promise.resolve();
  if (googleMapsLoaderPromise) return googleMapsLoaderPromise;

  googleMapsLoaderPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places&v=weekly`;
    script.async = true;
    script.defer = true;
    script.onload = () => {
      isLoaded = true;
      resolve();
    };
    script.onerror = () => {
      googleMapsLoaderPromise = null;
      reject(new Error('Failed to load Google Maps script'));
    };
    document.head.appendChild(script);
  });

  return googleMapsLoaderPromise;
}

export function isGoogleMapsLoaded(): boolean {
  return isLoaded && typeof window.google !== 'undefined' && !!window.google.maps;
}

declare global {
  interface Window {
    google?: typeof google;
  }
}
