    public static boolean isArrow(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ItemTags.ARROWS) || stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW);
    }

    private boolean hasArrows() {
        if (isArrow(this.getItemInHand(InteractionHand.OFF_HAND))) return true;
        net.minecraft.world.SimpleContainer inv = this.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isArrow(inv.getItem(i))) return true;
        }
        return false;
    }
