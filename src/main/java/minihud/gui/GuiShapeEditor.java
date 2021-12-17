package minihud.gui;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import malilib.config.option.OptionListConfig;
import malilib.config.value.BlockSnap;
import malilib.gui.BaseScreen;
import malilib.gui.config.BaseConfigScreen;
import malilib.gui.edit.BaseLayerRangeEditScreen;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.listener.DoubleModifierButtonListener;
import malilib.gui.listener.DoubleTextFieldListener;
import malilib.gui.listener.IntegerModifierButtonListener;
import malilib.gui.listener.IntegerTextFieldListener;
import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.ColorIndicatorWidget;
import malilib.gui.widget.DoubleTextFieldWidget;
import malilib.gui.widget.IntegerTextFieldWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.Vec3dEditWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.gui.widget.button.OptionListConfigButton;
import malilib.input.ActionResult;
import malilib.util.ListUtils;
import malilib.util.data.DualDoubleConsumer;
import malilib.util.data.DualIntConsumer;
import malilib.util.position.Direction;
import malilib.util.position.Vec3d;
import minihud.Reference;
import minihud.renderer.shapes.ShapeBase;
import minihud.renderer.shapes.ShapeBox;
import minihud.renderer.shapes.ShapeCircle;
import minihud.renderer.shapes.ShapeCircleBase;
import minihud.renderer.shapes.ShapeManager;
import minihud.renderer.shapes.ShapeSpawnSphere;
import minihud.util.value.ShapeRenderType;
import net.minecraft.util.math.AxisAlignedBB;

public class GuiShapeEditor extends BaseLayerRangeEditScreen
{
    private final ShapeBase shape;
    private final OptionListConfig<BlockSnap> configBlockSnap;

    public GuiShapeEditor(ShapeBase shape)
    {
        super("minihud_shape_editor", ConfigScreen.ALL_TABS, ConfigScreen.SHAPES, shape.getLayerRange());

        this.shape = shape;
        this.configBlockSnap = new OptionListConfig<>("blockSnap", BlockSnap.NONE, BlockSnap.VALUES, "");

        this.setTitle("minihud.title.screen.shape_editor", Reference.MOD_VERSION);
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        int x = 10;
        int y = 20;

        this.createShapeEditorElements(x, y);

        GenericButton button = GenericButton.create(ConfigScreen.SHAPES.getDisplayName());
        button.setPosition(x, this.height - 24);
        button.setActionListener(() -> {
            BaseConfigScreen.setCurrentTab(Reference.MOD_ID, ConfigScreen.SHAPES);
            BaseScreen.openScreen(new ShapeManagerScreen());
        });
        this.addWidget(button);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        this.editWidget.setPosition(this.x + 142, this.y + 142);
    }

    private void createColorInput(int x, int y)
    {
        LabelWidget label = new LabelWidget("minihud.label.shapes.color");
        label.setPosition(x, y + 1);
        this.addWidget(label);
        y += 12;

        BaseTextFieldWidget txtField = new BaseTextFieldWidget(70, 16, String.format("#%08X", this.shape.getColor().intValue));
        txtField.setPosition(x, y);
        txtField.setTextValidator(BaseTextFieldWidget.VALIDATOR_HEX_COLOR_8_6_4_3);
        txtField.setListener(this.shape::setColorFromString);
        this.addWidget(txtField);

        ColorIndicatorWidget ci = new ColorIndicatorWidget(18, 18, this.shape.getColor().intValue, this.shape::setColor);
        ci.setPosition(x + 74, y - 1);
        this.addWidget(ci);
    }

    private void createShapeEditorElements(int x, int y)
    {
        LabelWidget label = new LabelWidget("minihud.label.shapes.display_name");
        label.setPosition(x, y + 1);
        this.addWidget(label);
        y += 12;

        BaseTextFieldWidget textField = new BaseTextFieldWidget(240, 16, this.shape.getDisplayName());
        textField.setPosition(x, y);
        textField.setListener(this.shape::setDisplayName);
        this.addWidget(textField);
        y += 20;

        int renderTypeX = x + 230;
        int renderTypeY = y + 2;

        switch (this.shape.getType())
        {
            case BOX:
                this.createShapeEditorElementsBox(x, y);
                // TODO: move this
                this.editWidget.setPosition(x, y + 300);
                break;
            case CAN_DESPAWN_SPHERE:
            case CAN_SPAWN_SPHERE:
            case DESPAWN_SPHERE:
            {
                ShapeSpawnSphere shape = (ShapeSpawnSphere) this.shape;
                this.createShapeEditorElementsSphereBase(x, y, false);
                this.createShapeEditorElementDoubleField(x + 150, y + 2, shape::getMargin, shape::setMargin, "minihud.label.shapes.margin", false);
                break;
            }

            case CIRCLE:
            {
                ShapeCircle shape = (ShapeCircle) this.shape;
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createShapeEditorElementIntField(x + 120, y + 38, shape::getHeight, shape::setHeight, "minihud.label.shapes.height", true);
                this.createDirectionButton(x + 230, y + 36, shape::getMainAxis, shape::setMainAxis, "minihud.button.shapes.circle.main_axis");
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.button.shapes.render_type");
                break;
            }

            case SPHERE_BLOCKY:
                this.createShapeEditorElementsSphereBase(x, y, true);
                this.createRenderTypeButton(renderTypeX, renderTypeY, this.shape::getRenderType, this.shape::setRenderType, "minihud.button.shapes.render_type");
                break;
        }
    }

    private void createShapeEditorElementsBox(int xIn, int yIn)
    {
        ShapeBox shape = (ShapeBox) this.shape;

        int x = xIn;
        int y = yIn + 4;

        createBoxInputs(x, y, x, y + 88, 110, shape::getBox, shape::setBox);

        y += 184;
        this.createColorInput(x, y);

        x = xIn + 210;
        y = yIn + 4;
        this.addBoxSideToggleCheckbox(x, y     , Direction.DOWN,  shape);
        this.addBoxSideToggleCheckbox(x, y + 11, Direction.UP,    shape);
        this.addBoxSideToggleCheckbox(x, y + 22, Direction.NORTH, shape);
        this.addBoxSideToggleCheckbox(x, y + 33, Direction.SOUTH, shape);
        this.addBoxSideToggleCheckbox(x, y + 44, Direction.WEST,  shape);
        this.addBoxSideToggleCheckbox(x, y + 55, Direction.EAST,  shape);

        x = xIn + 120;
        y = yIn + 4;

        if (shape.isGridEnabled())
        {
            LabelWidget label = new LabelWidget("minihud.label.shape_box.grid_size");
            label.setPosition(x, y);
            this.addWidget(label);

            Vec3dEditWidget editGridSize = new Vec3dEditWidget(80, 50, 2, false, shape.getGridSize(), shape::setGridSize);
            editGridSize.setPosition(x, y + 12);
            this.addWidget(editGridSize);

            y += 80;

            label = new LabelWidget("minihud.label.shape_box.grid_start_offset");
            label.setPosition(x, y);
            this.addWidget(label);

            Vec3dEditWidget editGridStartOffset = new Vec3dEditWidget(80, 50, 2, false, shape.getGridStartOffset(), shape::setGridStartOffset);
            editGridStartOffset.setPosition(x, y + 12);
            this.addWidget(editGridStartOffset);


            label = new LabelWidget("minihud.label.shape_box.grid_end_offset");
            label.setPosition(x + 100, y);
            this.addWidget(label);

            Vec3dEditWidget editGridEndOffset = new Vec3dEditWidget(80, 50, 2, false, shape.getGridEndOffset(), shape::setGridEndOffset);
            editGridEndOffset.setPosition(x + 100, y + 12);
            this.addWidget(editGridEndOffset);
        }

        GenericButton button = new OnOffButton(-1, 20, OnOffButton.OnOffStyle.TEXT_ON_OFF, shape::isGridEnabled, "minihud.label.shape_box.grid_enabled");
        button.setPosition(x, yIn + 180);
        button.setActionListener(() -> this.toggleGridEnabled(shape));
        this.addWidget(button);
    }

    private void toggleGridEnabled(ShapeBox shape)
    {
        shape.toggleGridEnabled();
        this.initGui();
    }

    private void addBoxSideToggleCheckbox(int x, int y, Direction side, ShapeBox shape)
    {
        CheckBoxWidget cb = new CheckBoxWidget(
            this.capitalize(side.getName()),
            "Render the " + side.getName() + " side of the box",
            () -> shape.isSideEnabled(side),
            (enabled) -> this.toggleSideEnabled(side, shape)
        );
        cb.setPosition(x, y);
        this.addWidget(cb);
    }

    private void toggleSideEnabled(Direction side, ShapeBox shape)
    {
        int mask = shape.getEnabledSidesMask();
        shape.setEnabledSidesMask(mask ^ (1 << side.getIndex()));
    }

    private String capitalize(String str)
    {
        if (str.length() > 1)
        {
            return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
        }

        return str.length() > 0 ? str.toUpperCase(Locale.ROOT) : str;
    }

    private void createShapeEditorElementsSphereBase(int x, int y, boolean addRadiusInput)
    {
        ShapeCircleBase shape = (ShapeCircleBase) this.shape;

        LabelWidget label = new LabelWidget("minihud.label.shapes.center");
        label.setPosition(x, y + 2);
        this.addWidget(label);

        Vec3dEditWidget editWidget = new Vec3dEditWidget(120, 72, 2, true, shape.getCenter(), shape::setCenter);
        editWidget.setPosition(x, y + 12);
        this.addWidget(editWidget);

        if (addRadiusInput)
        {
            this.createShapeEditorElementDoubleField(editWidget.getRight(), y + 2, shape::getRadius, shape::setRadius, "minihud.label.shapes.radius", true);
        }

        x += 11;
        y += 66;

        this.configBlockSnap.setValue(shape.getBlockSnap());

        OptionListConfigButton buttonSnap = new OptionListConfigButton(-1, 20, this.configBlockSnap, "minihud.button.shapes.block_snap");
        buttonSnap.setPosition(editWidget.getRight() + 12, y);
        buttonSnap.setChangeListener(this::onBlockSnapChanged);
        this.addWidget(buttonSnap);

        y += 24;

        this.createColorInput(x, y);
    }

    protected void onBlockSnapChanged()
    {
        ((ShapeCircleBase) this.shape).setBlockSnap(this.configBlockSnap.getValue());
        this.initScreen();
    }

    private void createShapeEditorElementDoubleField(int x, int y, DoubleSupplier supplier,
                                                     DoubleConsumer consumer, String translationKey, boolean addButton)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x + 12, y);
        this.addWidget(label);
        y += 10;

        DoubleTextFieldWidget txtField = new DoubleTextFieldWidget(40, 16, supplier.getAsDouble());
        txtField.setPosition(x + 12, y);
        txtField.setListener(new DoubleTextFieldListener(consumer));
        txtField.setUpdateListenerAlways(true);
        this.addWidget(txtField);

        if (addButton)
        {
            GenericButton button = GenericButton.create(DefaultIcons.BTN_PLUSMINUS_16);
            button.setPosition(x + 54, y);
            button.setActionListener(new DoubleModifierButtonListener(supplier, new DualDoubleConsumer(consumer, (val) -> txtField.setText(String.valueOf(supplier.getAsDouble())) )));
            button.setCanScrollToClick(true);
            button.translateAndAddHoverString("malilib.gui.button.hover.plus_minus_tip");
            this.addWidget(button);
        }
    }

    private void createShapeEditorElementIntField(int x, int y, IntSupplier supplier, IntConsumer consumer,
                                                  String translationKey, boolean addButton)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x + 12, y);
        this.addWidget(label);
        y += 10;

        IntegerTextFieldWidget txtField = new IntegerTextFieldWidget(40, 16, supplier.getAsInt());
        txtField.setPosition(x + 12, y);
        txtField.setListener(new IntegerTextFieldListener(consumer));
        txtField.setUpdateListenerAlways(true);
        this.addWidget(txtField);

        if (addButton)
        {
            GenericButton button = GenericButton.create(DefaultIcons.BTN_PLUSMINUS_16);
            button.setPosition(x + 54, y);
            button.setActionListener(new IntegerModifierButtonListener(supplier, new DualIntConsumer(consumer, (val) -> txtField.setText(String.valueOf(supplier.getAsInt())) )));
            button.setCanScrollToClick(true);
            button.translateAndAddHoverString("malilib.gui.button.hover.plus_minus_tip");
            this.addWidget(button);
        }
    }

    private void createDirectionButton(int x, int y, Supplier<Direction> supplier,
                                       Consumer<Direction> consumer, String translationKey)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x, y);
        this.addWidget(label);
        y += 10;

        String name = org.apache.commons.lang3.StringUtils.capitalize(supplier.get().toString().toLowerCase());
        GenericButton button = GenericButton.create(50, 20, name);
        button.setActionListener((btn) -> { consumer.accept(supplier.get().cycle(btn == 1)); this.initGui(); return true; });
        button.setPosition(x, y);

        this.addWidget(button);
    }

    private void createRenderTypeButton(int x, int y, Supplier<ShapeRenderType> supplier,
                                        Consumer<ShapeRenderType> consumer, String translationKey)
    {
        LabelWidget label = new LabelWidget(translationKey);
        label.setPosition(x, y);
        this.addWidget(label);
        y += 10;

        GenericButton button = GenericButton.create(supplier.get().getDisplayName());
        button.setActionListener((btn) -> { consumer.accept(ListUtils.getNextEntry(ShapeRenderType.VALUES, supplier.get(), btn != 0)); this.initGui(); return true; });
        button.setPosition(x, y);
        this.addWidget(button);
    }

    public void createBoxInputs(int x1, int y1, int x2, int y2, int textFieldWidth,
                                Supplier<AxisAlignedBB> supplier, Consumer<AxisAlignedBB> consumer)
    {
        AxisAlignedBB box = supplier.get();
        MutableWrapperBox mutableBox = new MutableWrapperBox(box, consumer);

        LabelWidget minLabel = new LabelWidget("minihud.label.shape_box.minimum_coord");
        minLabel.setPosition(x1, y1);
        this.addWidget(minLabel);

        Vec3dEditWidget corner1Edit = new Vec3dEditWidget(textFieldWidth, 72, 2, true, mutableBox.getCorner1(), mutableBox::setCorner1);
        corner1Edit.setPosition(x1, y1 + 12);
        this.addWidget(corner1Edit);

        LabelWidget maxLabel = new LabelWidget("minihud.label.shape_box.maximum_coord");
        maxLabel.setPosition(x1, y2);
        this.addWidget(maxLabel);

        Vec3dEditWidget corner2Edit = new Vec3dEditWidget(textFieldWidth, 72, 2, true, mutableBox.getCorner2(), mutableBox::setCorner2);
        corner2Edit.setPosition(x2, y2 + 12);
        this.addWidget(corner2Edit);
    }

    public static ActionResult openShapeEditor()
    {
        ShapeBase shape = ShapeManager.INSTANCE.getSelectedShape();
        BaseScreen screen = shape != null ? new GuiShapeEditor(shape) : ShapeManagerScreen.openShapeManagerScreen();
        BaseScreen.openScreen(screen);
        return ActionResult.SUCCESS;
    }

    // TODO: move to malilib?
    public static class MutableWrapperBox
    {
        protected final Consumer<AxisAlignedBB> boxConsumer;
        protected double x1;
        protected double y1;
        protected double z1;
        protected double x2;
        protected double y2;
        protected double z2;

        public MutableWrapperBox(AxisAlignedBB box, Consumer<AxisAlignedBB> boxConsumer)
        {
            this.x1 = box.minX;
            this.y1 = box.minY;
            this.z1 = box.minZ;
            this.x2 = box.maxX;
            this.y2 = box.maxY;
            this.z2 = box.maxZ;
            this.boxConsumer = boxConsumer;
        }

        public Vec3d getCorner1() {
            return Vec3d.of(x1, y1, z1);
        }

        public Vec3d getCorner2() {
            return Vec3d.of(x2, y2, z2);
        }

        public void setCorner1(Vec3d corner) {
            this.x1 = corner.x;
            this.y1 = corner.y;
            this.z1 = corner.z;
            this.updateAxisAlignedBB();
        }

        public void setCorner2(Vec3d corner) {
            this.x2 = corner.x;
            this.y2 = corner.y;
            this.z2 = corner.z;
            this.updateAxisAlignedBB();
        }

        protected void updateAxisAlignedBB()
        {
            AxisAlignedBB box = new AxisAlignedBB(
                Math.min(this.x1, this.x2),
                Math.min(this.y1, this.y2),
                Math.min(this.z1, this.z2),
                Math.max(this.x1, this.x2),
                Math.max(this.y1, this.y2),
                Math.max(this.z1, this.z2)
            );
            this.boxConsumer.accept(box);
        }
    }

}
