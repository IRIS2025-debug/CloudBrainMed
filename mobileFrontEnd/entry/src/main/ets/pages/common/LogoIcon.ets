// common/LogoIcon.ets
@Component
export struct LogoIcon {
  @Prop iconSize: number = 60;
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private canvasContext: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);

  private drawLogo(): void {
    const ctx = this.canvasContext;
    const size = this.iconSize;
    const s = size / 28; // 缩放因子

    // 清空画布
    ctx.clearRect(0, 0, size, size);

    // ===== 1. 绘制圆角矩形背景 #2563eb =====
    const r = 8 * s;
    ctx.beginPath();
    ctx.moveTo(r, 0);
    ctx.lineTo(size - r, 0);
    ctx.quadraticCurveTo(size, 0, size, r);
    ctx.lineTo(size, size - r);
    ctx.quadraticCurveTo(size, size, size - r, size);
    ctx.lineTo(r, size);
    ctx.quadraticCurveTo(0, size, 0, size - r);
    ctx.lineTo(0, r);
    ctx.quadraticCurveTo(0, 0, r, 0);
    ctx.closePath();
    ctx.fillStyle = '#2563eb';
    ctx.fill();

    // ===== 2. 绘制心电图折线 =====
    // 原始SVG: M6 14 L10 10 L14 16 L18 8 L22 14 L26 12
    // 线宽: 1.8, 颜色: #ffffff, 圆头圆角
    ctx.beginPath();
    ctx.moveTo(6 * s, 14 * s);
    ctx.lineTo(10 * s, 10 * s);
    ctx.lineTo(14 * s, 16 * s);
    ctx.lineTo(18 * s, 8 * s);
    ctx.lineTo(22 * s, 14 * s);
    ctx.lineTo(26 * s, 12 * s);

    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 1.8 * s;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke();
  }

  build() {
    Canvas(this.canvasContext)
      .width(this.iconSize)
      .height(this.iconSize)
      .onReady(() => {
        this.drawLogo();
      })
  }
}