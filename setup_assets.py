import os
import wave
import struct
import math

base_dir = r"C:\Users\Haraprasad\.gemini\antigravity\Projects\FretForge"

def generate_wav(filename, freq=1000, duration=0.08, sample_rate=44100):
    n_samples = int(duration * sample_rate)
    path = os.path.join(base_dir, "app/src/main/res/raw", filename)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, 'w') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sample_rate)
        
        for i in range(n_samples):
            envelope = math.exp(-15.0 * i / n_samples)
            value = int(16000.0 * math.sin(2.0 * math.pi * freq * i / sample_rate) * envelope)
            data = struct.pack('<h', value)
            w.writeframesraw(data)

generate_wav("click_normal.wav", freq=800)
generate_wav("click_accent.wav", freq=1500)
print("Audio generated.")
