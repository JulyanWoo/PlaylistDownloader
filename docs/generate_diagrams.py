import matplotlib.pyplot as plt
import matplotlib.patches as patches
from PIL import Image, ImageDraw, ImageFont
import os

def generate_architecture_diagram():
    fig, ax = plt.subplots(figsize=(16, 11), dpi=220)
    fig.patch.set_facecolor('#0d1117')
    ax.set_facecolor('#0d1117')
    ax.set_xlim(0, 16)
    ax.set_ylim(0, 11)
    ax.axis('off')

    # Main Title
    ax.text(8.0, 10.4, "PlaylistDownloader - Architecture System Design", 
            ha='center', va='center', fontsize=20, fontweight='bold', color='#58a6ff', fontfamily='sans-serif')
    ax.text(8.0, 10.05, "Decoupled Event-Driven MVVM Pattern • Parallel Producer-Consumer Pipeline • Local AI Intelligence", 
            ha='center', va='center', fontsize=11, color='#8b949e', fontfamily='sans-serif')

    def draw_card(x, y, w, h, title, subtitle, items, border_color='#30363d', title_color='#58a6ff', badge_bg='#21262d'):
        rect = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08,rounding_size=0.18",
                                      facecolor='#161b22', edgecolor=border_color, linewidth=1.6)
        ax.add_patch(rect)
        
        header_rect = patches.FancyBboxPatch((x, y + h - 0.5), w, 0.5, boxstyle="round,pad=0.04,rounding_size=0.12",
                                             facecolor=badge_bg, edgecolor='none')
        ax.add_patch(header_rect)
        ax.text(x + 0.25, y + h - 0.26, title, fontsize=11, fontweight='bold', color=title_color, va='center')
        if subtitle:
            ax.text(x + w - 0.25, y + h - 0.26, subtitle, fontsize=8.8, color='#8b949e', ha='right', va='center')

        line_y = y + h - 0.85
        for item in items:
            ax.text(x + 0.3, line_y, f"• {item}", fontsize=9.0, color='#c9d1d9', va='center')
            line_y -= 0.35

    # Layer 1: UI / Presentation Layer (Top Left)
    draw_card(0.8, 6.9, 6.8, 2.7, "UI & Presentation Layer", "JavaFX 17 + AtlantaFX Primer Dark", [
        "WelcomeController & MainController (Multi-URL Ingestion)",
        "QueueController (Download Cards & Real-time Progress)",
        "ProgressController & ProgressManager (ETA & Metric Gauges)",
        "LibraryAnalyzerController & TableControls (Audio Player Preview)",
        "LogsController (O(1) Ring Buffer Real-time Stream Viewer)",
        "Custom WindowStageDecorator (Frameless Window Controls)"
    ], border_color='#58a6ff', title_color='#79c0ff', badge_bg='#1f2d40')

    # Layer 2: Reactive & State Layer (Top Right)
    draw_card(8.4, 6.9, 6.8, 2.7, "Reactive Communication & State", "MVVM Pattern + Pub-Sub", [
        "EventBus: Low-Latency In-Memory Decoupled Pub-Sub",
        "DownloadEvent & LibraryAnalyzerEvent Broadcast Pipeline",
        "MainViewModel & LibraryAnalyzerViewModel (Reactive UI Bindings)",
        "ServiceFactory: Strict Dependency Injection & Graceful Disposal",
        "MainViewBinder & LibraryAnalyzerViewBinder (FXML Glue)",
        "DialogService & FolderChooserService (OS Modal Handlers)"
    ], border_color='#bc8cff', title_color='#d2a8ff', badge_bg='#2b1f40')

    # Layer 3: Download & Conversion Core (Middle Left)
    draw_card(0.8, 3.3, 6.8, 3.1, "High-Throughput Download Engine", "Parallel Producer-Consumer Pipeline", [
        "MainDownloadFacade: Unified Orchestration Facade for UI",
        "DownloadCoordinator: Non-blocking Queue Poller & Worker Pool",
        "YtDlpCommandBuilder: Zero-Collision Raw Templates & Auto-Resume",
        "YouTubeDownloadService: High-Speed Raw Stream Ingestion (100+ MB/s)",
        "AudioConversionService: Parallel FFmpeg Worker Pool (320kbps MP3)",
        "ProcessExecutor: Full Process Tree Termination & Suspend/Resume",
        "BinaryResolver & YtDlpUpdateService: Self-Healing Binary Updater"
    ], border_color='#3fb950', title_color='#56d364', badge_bg='#1b3222')

    # Layer 4: Library Intelligence Core (Middle Right)
    draw_card(8.4, 3.3, 6.8, 3.1, "Local Intelligence & De-duplication", "Multi-Phase Fuzzy AI & Tagging", [
        "LibraryAnalyzerFacade: High-Performance Parallel File Crawler",
        "SongMetadataReader: JAudioTagger ID3/Flac/Ogg Metadata Parsing",
        "ExactHashDetector: MD5 / SHA-256 Content Fingerprinting",
        "DuplicateSimilarityService: Hybrid Levenshtein + Jaccard + N-Grams",
        "LanguageDetectorService & MiniLanguageModel: Trained Char-Gram AI",
        "Smart Strategies: QualityFirst, LargestFile, OldestFile Selection",
        "AudioPreviewService: Native JavaFX MediaPlayer In-Line Audition"
    ], border_color='#f0883e', title_color='#ffa657', badge_bg='#382618')

    # Layer 5: Native Binaries & Storage (Bottom)
    draw_card(0.8, 0.5, 14.4, 2.2, "Native Operating System & Execution Layer", "Hardware & Subsystems", [
        "yt-dlp Binary Engine: Video/Audio Stream Extraction, Playlist Chunking & Rapid Ingestion",
        "FFmpeg Binary Engine: High-Fidelity Audio Transcoding, Re-sampling, Artwork & ID3 Tag Embedding",
        "Local File Storage: Output Audio Library, user-config.properties, app.properties, Ring-Buffer Log Files",
        "Distribution Packaging: Launch4j Executable (.exe) & JPackage Standalone Runtime Bundler"
    ], border_color='#8b949e', title_color='#c9d1d9', badge_bg='#21262d')

    # Connectors with good clearance
    # UI <-> Reactive State
    ax.annotate("", xy=(8.4, 8.25), xytext=(7.6, 8.25),
                arrowprops=dict(arrowstyle="<->,head_width=0.35,head_length=0.45", color='#bc8cff', lw=2))
    ax.text(8.0, 8.45, "EventBus", color='#bc8cff', fontsize=8.5, fontweight='bold', ha='center')

    # UI -> Download Engine
    ax.annotate("", xy=(4.2, 6.4), xytext=(4.2, 6.9),
                arrowprops=dict(arrowstyle="->,head_width=0.35,head_length=0.45", color='#58a6ff', lw=2))

    # Reactive -> Analyzer
    ax.annotate("", xy=(11.8, 6.4), xytext=(11.8, 6.9),
                arrowprops=dict(arrowstyle="->,head_width=0.35,head_length=0.45", color='#f0883e', lw=2))

    # Download Engine -> Native
    ax.annotate("", xy=(4.2, 2.7), xytext=(4.2, 3.3),
                arrowprops=dict(arrowstyle="<->,head_width=0.35,head_length=0.45", color='#3fb950', lw=2))
    ax.text(4.4, 3.0, "Spawns Processes", color='#3fb950', fontsize=8, ha='left', va='center')

    # Analyzer -> Local Files
    ax.annotate("", xy=(11.8, 2.7), xytext=(11.8, 3.3),
                arrowprops=dict(arrowstyle="<->,head_width=0.35,head_length=0.45", color='#ffa657', lw=2))
    ax.text(11.6, 3.0, "I/O Disk Scanning", color='#ffa657', fontsize=8, ha='right', va='center')

    plt.tight_layout()
    plt.savefig('docs/images/architecture_diagram.png', dpi=220, bbox_inches='tight', facecolor='#0d1117')
    plt.close()
    print("architecture_diagram.png updated.")


def generate_pipeline_diagram():
    fig, ax = plt.subplots(figsize=(15, 7), dpi=220)
    fig.patch.set_facecolor('#0d1117')
    ax.set_facecolor('#0d1117')
    ax.set_xlim(0, 15)
    ax.set_ylim(0, 7)
    ax.axis('off')

    # Title
    ax.text(7.5, 6.5, "Asynchronous Producer-Consumer Media Pipeline", 
            ha='center', va='center', fontsize=18, fontweight='bold', color='#58a6ff', fontfamily='sans-serif')
    ax.text(7.5, 6.15, "Non-blocking Streaming Ingestion (yt-dlp) decoupled from Parallel Transcoding (FFmpeg)", 
            ha='center', va='center', fontsize=10.5, color='#8b949e', fontfamily='sans-serif')

    steps = [
        {
            "num": "01",
            "title": "Ingestion & Validation",
            "badge": "UI & Facade",
            "color": "#58a6ff",
            "bg": "#1f2d40",
            "lines": [
                "Batch URL Parser",
                "Regex URL Validation",
                "Fuzzy Dupe Pre-check",
                "SongMetadataService",
                "Queue Registration"
            ],
            "x": 0.6
        },
        {
            "num": "02",
            "title": "Raw Stream Producer",
            "badge": "yt-dlp Engine",
            "color": "#3fb950",
            "bg": "#1b3222",
            "lines": [
                "Dynamic ID Template",
                "Zero-Collision Naming",
                "Peak Speeds (70-100+ MB/s)",
                "Real-time Stream Parser",
                "Auto-Backoff & Resume"
            ],
            "x": 4.2
        },
        {
            "num": "03",
            "title": "Parallel Transcoder",
            "badge": "FFmpeg Worker Pool",
            "color": "#bc8cff",
            "bg": "#2b1f40",
            "lines": [
                "Thread Pool Workers",
                "MP3 320 kbps Encoding",
                "Audio Normalization",
                "Process Tree Isolation",
                "Dynamic Concurrency"
            ],
            "x": 7.8
        },
        {
            "num": "04",
            "title": "Metadata & Library Sync",
            "badge": "Tagging & Disk",
            "color": "#f0883e",
            "bg": "#382618",
            "lines": [
                "ID3v2.4 Tag Injection",
                "HD Album Art Embedding",
                "Temporary File Cleanup",
                "EventBus Notification",
                "Completed State in UI"
            ],
            "x": 11.4
        }
    ]

    for s in steps:
        x, y, w, h = s["x"], 1.4, 3.0, 4.3
        rect = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08,rounding_size=0.18",
                                      facecolor='#161b22', edgecolor=s["color"], linewidth=1.8)
        ax.add_patch(rect)
        
        badge_rect = patches.FancyBboxPatch((x + 0.15, y + h - 0.55), 0.7, 0.4, boxstyle="round,pad=0.03,rounding_size=0.08",
                                            facecolor=s["bg"], edgecolor=s["color"], linewidth=1.2)
        ax.add_patch(badge_rect)
        ax.text(x + 0.5, y + h - 0.35, s["num"], fontsize=9.5, fontweight='bold', color=s["color"], ha='center', va='center')
        ax.text(x + 1.0, y + h - 0.35, s["title"], fontsize=10, fontweight='bold', color='#f0f6fc', va='center')
        
        pill = patches.FancyBboxPatch((x + 0.25, y + h - 1.0), w - 0.5, 0.32, boxstyle="round,pad=0.03,rounding_size=0.08",
                                      facecolor='#21262d', edgecolor='none')
        ax.add_patch(pill)
        ax.text(x + w / 2, y + h - 0.84, s["badge"], fontsize=8.5, fontweight='bold', color=s["color"], ha='center', va='center')

        line_y = y + h - 1.45
        for line in s["lines"]:
            ax.text(x + 0.3, line_y, f"• {line}", fontsize=8.8, color='#c9d1d9', va='center')
            line_y -= 0.42

        pbar_bg = patches.FancyBboxPatch((x + 0.25, y + 0.35), w - 0.5, 0.15, boxstyle="round,pad=0.02,rounding_size=0.05",
                                         facecolor='#21262d', edgecolor='none')
        ax.add_patch(pbar_bg)
        pbar_fill = patches.FancyBboxPatch((x + 0.25, y + 0.35), (w - 0.5) * (int(s["num"]) / 4.0), 0.15, 
                                           boxstyle="round,pad=0.02,rounding_size=0.05", facecolor=s["color"], edgecolor='none')
        ax.add_patch(pbar_fill)

    for i in range(len(steps) - 1):
        x1 = steps[i]["x"] + 3.0 + 0.08
        x2 = steps[i+1]["x"] - 0.08
        mid_y = 3.55
        ax.annotate("", xy=(x2, mid_y), xytext=(x1, mid_y),
                    arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.5", color='#58a6ff', lw=2.2))

    note_rect = patches.FancyBboxPatch((0.6, 0.3), 13.8, 0.75, boxstyle="round,pad=0.05,rounding_size=0.1",
                                       facecolor='#161b22', edgecolor='#30363d', linewidth=1.2)
    ax.add_patch(note_rect)
    ax.text(7.5, 0.67, "High-Performance Concurrency Advantage:", 
            fontsize=9.5, fontweight='bold', color='#58a6ff', ha='center', va='center')
    ax.text(7.5, 0.45, "The network is never blocked waiting for CPU-intensive MP3 encoding; downloads stream continuously at maximum bandwidth.", 
            fontsize=8.5, color='#8b949e', ha='center', va='center')

    plt.tight_layout()
    plt.savefig('docs/images/pipeline_diagram.png', dpi=220, bbox_inches='tight', facecolor='#0d1117')
    plt.close()
    print("pipeline_diagram.png updated.")


def generate_analyzer_diagram():
    fig, ax = plt.subplots(figsize=(15, 8.5), dpi=220)
    fig.patch.set_facecolor('#0d1117')
    ax.set_facecolor('#0d1117')
    ax.set_xlim(0, 15)
    ax.set_ylim(0, 8.5)
    ax.axis('off')

    ax.text(7.5, 8.0, "Smart Library Analyzer & Duplicate Detection Workflow", 
            ha='center', va='center', fontsize=18, fontweight='bold', color='#f0883e', fontfamily='sans-serif')
    ax.text(7.5, 7.65, "Multi-Phase Audio Intelligence: Hash Fingerprinting, Fuzzy N-Gram Clustering & In-Line Playback", 
            ha='center', va='center', fontsize=10.5, color='#8b949e', fontfamily='sans-serif')

    stages = [
        {
            "title": "1. Recursive Crawler & Tagging",
            "badge": "SongMetadataReader",
            "color": "#58a6ff",
            "bg": "#1f2d40",
            "items": [
                "Parallel scan of MP3/FLAC/M4A/WAV",
                "JAudioTagger: Title, Artist, Album",
                "Audio Properties: Bitrate, Duration",
                "Fast File Size & Timestamp Indexing",
                "Cache Manager for Instant Rescans"
            ],
            "x": 0.8, "y": 4.4, "w": 6.2, "h": 2.7
        },
        {
            "title": "2. Multi-Stage Duplicate Engine",
            "badge": "ExactHash + Fuzzy Matcher",
            "color": "#bc8cff",
            "bg": "#2b1f40",
            "items": [
                "Tier A: MD5 / SHA-256 Exact Hash",
                "Tier B: TitleNormalizer (Removes 'Official Video', diacritics)",
                "Tier C: Hybrid Levenshtein Distance (Typos/Edits)",
                "Tier D: Jaccard N-Gram Similarity (Word reorder)",
                "Classification: Exact, High Conf, Suspicious"
            ],
            "x": 8.0, "y": 4.4, "w": 6.2, "h": 2.7
        },
        {
            "title": "3. Language AI & Track Parser",
            "badge": "MiniLanguageModel N-Grams",
            "color": "#3fb950",
            "bg": "#1b3222",
            "items": [
                "Trained Char-Gram Language Model",
                "Multi-lingual classification (ES, EN, etc.)",
                "Regex Artist-Title Heuristics",
                "Featured Artists Extraction ('ft.', 'feat.')",
                "Language Filtering Browser Matrix"
            ],
            "x": 0.8, "y": 1.2, "w": 6.2, "h": 2.7
        },
        {
            "title": "4. Intelligent Resolution & Preview",
            "badge": "AudioPreviewService + Strategies",
            "color": "#ffa657",
            "bg": "#382618",
            "items": [
                "Native In-Table Audio Audition Player",
                "Selection Strategy: QualityFirst (Higher Bitrate)",
                "Selection Strategy: LargestFile (Uncompressed)",
                "Selection Strategy: OldestFile (Original Source)",
                "Batch Safe Trash / Permanent Delete Protection"
            ],
            "x": 8.0, "y": 1.2, "w": 6.2, "h": 2.7
        }
    ]

    for s in stages:
        x, y, w, h = s["x"], s["y"], s["w"], s["h"]
        rect = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08,rounding_size=0.18",
                                      facecolor='#161b22', edgecolor=s["color"], linewidth=1.6)
        ax.add_patch(rect)
        
        header_rect = patches.FancyBboxPatch((x, y + h - 0.48), w, 0.48, boxstyle="round,pad=0.04,rounding_size=0.12",
                                             facecolor=s["bg"], edgecolor='none')
        ax.add_patch(header_rect)
        ax.text(x + 0.25, y + h - 0.25, s["title"], fontsize=10.5, fontweight='bold', color=s["color"], va='center')
        ax.text(x + w - 0.25, y + h - 0.25, s["badge"], fontsize=8.5, color='#8b949e', ha='right', va='center')

        line_y = y + h - 0.82
        for item in s["items"]:
            ax.text(x + 0.3, line_y, f"• {item}", fontsize=8.8, color='#c9d1d9', va='center')
            line_y -= 0.35

    ax.annotate("", xy=(8.0, 5.75), xytext=(7.0, 5.75),
                arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.5", color='#58a6ff', lw=2.2))
    ax.text(7.5, 5.95, "Parsed Songs", color='#58a6ff', fontsize=8, ha='center')

    ax.annotate("", xy=(11.1, 3.9), xytext=(11.1, 4.4),
                arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.5", color='#ffa657', lw=2.2))
    ax.text(11.2, 4.15, "Clustered Groups", color='#ffa657', fontsize=8, va='center')

    ax.annotate("", xy=(3.9, 3.9), xytext=(3.9, 4.4),
                arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.5", color='#3fb950', lw=2.2))
    ax.text(4.0, 4.15, "Metadata Tokens", color='#3fb950', fontsize=8, va='center')

    ax.annotate("", xy=(8.0, 2.55), xytext=(7.0, 2.55),
                arrowprops=dict(arrowstyle="->,head_width=0.4,head_length=0.5", color='#3fb950', lw=2.2))
    ax.text(7.5, 2.75, "Language Tags", color='#3fb950', fontsize=8, ha='center')

    plt.tight_layout()
    plt.savefig('docs/images/analyzer_workflow.png', dpi=220, bbox_inches='tight', facecolor='#0d1117')
    plt.close()
    print("analyzer_workflow.png updated.")


def generate_hero_banner():
    banner_w, banner_h = 1200, 420
    banner = Image.new('RGBA', (banner_w, banner_h), (13, 17, 23, 255))
    draw = ImageDraw.Draw(banner)

    # Subtle gradient background line accents
    for i in range(banner_w):
        ratio = i / banner_w
        r = int(13 + ratio * (24 - 13))
        g = int(17 + ratio * (30 - 17))
        b = int(23 + ratio * (40 - 23))
        draw.line([(i, 0), (i, banner_h)], fill=(r, g, b, 255))

    # Outer glow behind circular logo
    glow_center_x, glow_center_y = 190, 210
    for rad in range(150, 15, -4):
        alpha = int((150 - rad) * 0.40)
        draw.ellipse([glow_center_x - rad, glow_center_y - rad, glow_center_x + rad, glow_center_y + rad],
                     fill=(88, 166, 255, alpha))

    # Paste Logo
    logo_path = 'docs/images/logo.png'
    if os.path.exists(logo_path):
        logo_img = Image.open(logo_path).convert('RGBA')
        logo_img = logo_img.resize((220, 220), Image.Resampling.LANCZOS)
        banner.paste(logo_img, (glow_center_x - 110, glow_center_y - 110), logo_img)

    try:
        font_title = ImageFont.truetype("arialbd.ttf", 48)
        font_sub = ImageFont.truetype("arial.ttf", 21)
        font_badge = ImageFont.truetype("arialbd.ttf", 14)
        font_desc = ImageFont.truetype("arial.ttf", 15)
    except Exception:
        font_title = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_badge = ImageFont.load_default()
        font_desc = ImageFont.load_default()

    # Product Title & Slogan
    draw.text((360, 75), "PlaylistDownloader", fill=(88, 166, 255), font=font_title)
    draw.text((360, 140), "Next-Generation Music Downloader & Library Intelligence Suite", fill=(240, 246, 252), font=font_sub)
    draw.text((360, 180), "Asynchronous Producer-Consumer Pipeline • 320 kbps MP3 Transcoding • Fuzzy AI De-duplication", 
              fill=(139, 148, 158), font=font_desc)

    # Crisp Clean Feature Badges (no missing glyph boxes)
    badges = [
        ("100+ MiB/s Stream", (31, 50, 34), (63, 185, 80)),
        ("Zero-Collision Naming", (31, 45, 64), (88, 166, 255)),
        ("Fuzzy AI Deduplication", (43, 31, 64), (188, 140, 255)),
        ("In-Line Audio Player", (56, 38, 24), (240, 136, 62)),
        ("Standalone JPackage .exe", (35, 38, 43), (201, 209, 217))
    ]

    bx = 360
    by = 245
    for text, bg_color, border_color in badges:
        txt_w = len(text) * 9 + 28
        draw.rounded_rectangle([bx, by, bx + txt_w, by + 34], radius=8, fill=bg_color, outline=border_color, width=1)
        # Small accent circle inside badge
        draw.ellipse([bx + 10, by + 12, bx + 18, by + 20], fill=border_color)
        draw.text((bx + 24, by + 8), text, fill=border_color, font=font_badge)
        bx += txt_w + 14
        if bx > banner_w - 200:
            bx = 360
            by += 44

    banner.save('docs/images/product_banner.png', 'PNG')
    print("product_banner.png updated.")


def generate_features_overview():
    fig, ax = plt.subplots(figsize=(15, 8), dpi=220)
    fig.patch.set_facecolor('#0d1117')
    ax.set_facecolor('#0d1117')
    ax.set_xlim(0, 15)
    ax.set_ylim(0, 8)
    ax.axis('off')

    ax.text(7.5, 7.5, "PlaylistDownloader - Core Capabilities & Feature Pillars", 
            ha='center', va='center', fontsize=18, fontweight='bold', color='#58a6ff', fontfamily='sans-serif')
    ax.text(7.5, 7.15, "Engineered for Extreme Speed, Reliability, Audio Fidelity, and Intelligent Organization", 
            ha='center', va='center', fontsize=10.5, color='#8b949e', fontfamily='sans-serif')

    pillars = [
        {
            "pillar": "PILLAR 1",
            "title": "Ultra-Fast Streaming Engine",
            "color": "#3fb950",
            "bg": "#1b3222",
            "points": [
                "Raw media streaming via yt-dlp at 70-100+ MiB/s",
                "Asynchronous Producer-Consumer architecture",
                "Parallel worker pool for FFmpeg MP3 transcoding",
                "Zero-Collision file naming (raw_id___title.ext)",
                "Chunked batching with backoff retry & resume"
            ],
            "x": 0.8, "y": 1.0, "w": 6.4, "h": 5.7
        },
        {
            "pillar": "PILLAR 2",
            "title": "Smart Library Intelligence",
            "color": "#f0883e",
            "bg": "#382618",
            "points": [
                "Local directory recursive music crawler",
                "JAudioTagger full metadata extraction (ID3, bitrate)",
                "Multi-tier duplicate detection (MD5, fuzzy n-grams)",
                "Trained character-gram language detector",
                "Native in-table audio preview audition player"
            ],
            "x": 7.8, "y": 1.0, "w": 6.4, "h": 5.7
        }
    ]

    for p in pillars:
        x, y, w, h = p["x"], p["y"], p["w"], p["h"]
        rect = patches.FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.08,rounding_size=0.18",
                                      facecolor='#161b22', edgecolor=p["color"], linewidth=1.8)
        ax.add_patch(rect)
        
        # Pill for Pillar
        pill = patches.FancyBboxPatch((x + 0.3, y + h - 0.6), 1.6, 0.36, boxstyle="round,pad=0.02,rounding_size=0.08",
                                      facecolor=p["bg"], edgecolor=p["color"], linewidth=1.2)
        ax.add_patch(pill)
        ax.text(x + 1.1, y + h - 0.42, p["pillar"], fontsize=8.5, fontweight='bold', color=p["color"], ha='center', va='center')

        # Title
        ax.text(x + 2.1, y + h - 0.42, p["title"], fontsize=11, fontweight='bold', color='#f0f6fc', va='center')

        # Divider line
        ax.plot([x + 0.3, x + w - 0.3], [y + h - 0.85, y + h - 0.85], color='#30363d', lw=1.2)

        line_y = y + h - 1.3
        for pt in p["points"]:
            # Feature check bullet
            check_box = patches.FancyBboxPatch((x + 0.35, line_y - 0.12), 0.24, 0.24, boxstyle="round,pad=0.02,rounding_size=0.06",
                                              facecolor=p["bg"], edgecolor=p["color"], linewidth=1)
            ax.add_patch(check_box)
            ax.text(x + 0.47, line_y, "✓", fontsize=7.5, fontweight='bold', color=p["color"], ha='center', va='center')
            ax.text(x + 0.75, line_y, pt, fontsize=9.2, color='#c9d1d9', va='center')
            line_y -= 0.82

    plt.tight_layout()
    plt.savefig('docs/images/features_overview.png', dpi=220, bbox_inches='tight', facecolor='#0d1117')
    plt.close()
    print("features_overview.png generated.")

if __name__ == '__main__':
    generate_architecture_diagram()
    generate_pipeline_diagram()
    generate_analyzer_diagram()
    generate_hero_banner()
    generate_features_overview()
