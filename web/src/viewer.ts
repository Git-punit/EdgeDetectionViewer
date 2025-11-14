/**
 * TypeScript Web Viewer for Edge Detection processed frames
 * Displays frame data with stats overlay
 */

interface FrameData {
    imageData: string; // base64 or URL
    width: number;
    height: number;
    fps: number;
    timestamp: number;
    processedWithEdges: boolean;
}

class EdgeDetectionViewer {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D;
    private statsElement: HTMLElement;
    private currentFrame: FrameData | null = null;

    constructor() {
        this.canvas = document.getElementById('frameCanvas') as HTMLCanvasElement;
        this.ctx = this.canvas.getContext('2d')!;
        this.statsElement = document.getElementById('stats')!;

        this.setupUI();
        this.loadSampleFrame();
    }

    private setupUI(): void {
        // Setup file input handler
        const fileInput = document.getElementById('fileInput') as HTMLInputElement;
        fileInput.addEventListener('change', (e) => this.handleFileUpload(e));

        // Setup demo button
        const demoButton = document.getElementById('demoButton') as HTMLButtonElement;
        demoButton.addEventListener('click', () => this.loadSampleFrame());

        // Setup clear button
        const clearButton = document.getElementById('clearButton') as HTMLButtonElement;
        clearButton.addEventListener('click', () => this.clearFrame());
    }

    private handleFileUpload(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];

        if (!file) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            const imageData = e.target?.result as string;
            
            // Create a temporary image to get dimensions
            const img = new Image();
            img.onload = () => {
                this.currentFrame = {
                    imageData: imageData,
                    width: img.width,
                    height: img.height,
                    fps: 30.0, // Mock FPS
                    timestamp: Date.now(),
                    processedWithEdges: true
                };
                this.renderFrame();
            };
            img.src = imageData;
        };
        reader.readAsDataURL(file);
    }

    private loadSampleFrame(): void {
        // Create a sample processed frame with edge-like pattern
        const width = 640;
        const height = 480;
        
        this.canvas.width = width;
        this.canvas.height = height;

        // Generate edge-detection-like pattern
        const imageData = this.ctx.createImageData(width, height);
        const data = imageData.data;

        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                const idx = (y * width + x) * 4;
                
                // Create edge-like pattern (checkerboard with some variations)
                const isEdge = (
                    Math.abs(Math.sin(x * 0.05) * Math.cos(y * 0.05)) > 0.8 ||
                    (x % 50 === 0) || 
                    (y % 50 === 0)
                );
                
                const value = isEdge ? 255 : 0;
                
                data[idx] = value;     // R
                data[idx + 1] = value; // G
                data[idx + 2] = value; // B
                data[idx + 3] = 255;   // A
            }
        }

        this.ctx.putImageData(imageData, 0, 0);

        this.currentFrame = {
            imageData: this.canvas.toDataURL(),
            width: width,
            height: height,
            fps: 15.5,
            timestamp: Date.now(),
            processedWithEdges: true
        };

        this.updateStats();
    }

    private renderFrame(): void {
        if (!this.currentFrame) return;

        const img = new Image();
        img.onload = () => {
            this.canvas.width = this.currentFrame!.width;
            this.canvas.height = this.currentFrame!.height;
            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            this.ctx.drawImage(img, 0, 0);
            this.updateStats();
        };
        img.src = this.currentFrame.imageData;
    }

    private updateStats(): void {
        if (!this.currentFrame) {
            this.statsElement.innerHTML = '<p class="no-data">No frame loaded</p>';
            return;
        }

        const date = new Date(this.currentFrame.timestamp);
        const processingMode = this.currentFrame.processedWithEdges ? 'Edge Detection' : 'Grayscale';

        this.statsElement.innerHTML = `
            <div class="stat-item">
                <span class="stat-label">Resolution:</span>
                <span class="stat-value">${this.currentFrame.width} × ${this.currentFrame.height}</span>
            </div>
            <div class="stat-item">
                <span class="stat-label">FPS:</span>
                <span class="stat-value">${this.currentFrame.fps.toFixed(1)}</span>
            </div>
            <div class="stat-item">
                <span class="stat-label">Processing:</span>
                <span class="stat-value">${processingMode}</span>
            </div>
            <div class="stat-item">
                <span class="stat-label">Timestamp:</span>
                <span class="stat-value">${date.toLocaleTimeString()}</span>
            </div>
        `;
    }

    private clearFrame(): void {
        this.currentFrame = null;
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.canvas.width = 640;
        this.canvas.height = 480;
        this.ctx.fillStyle = '#1a1a1a';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
        this.updateStats();
    }

    public loadFrameFromData(frameData: FrameData): void {
        this.currentFrame = frameData;
        this.renderFrame();
    }
}

// Initialize viewer when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new EdgeDetectionViewer();
});

// Export for potential external use
export { EdgeDetectionViewer, FrameData };
