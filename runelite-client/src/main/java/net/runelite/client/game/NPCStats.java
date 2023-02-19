package net.runelite.client.game;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public final class NPCStats {
    private final String name;

    private final String wiki;

    private final int hitpoints;

    private final int combatLevel;

    private final int slayerLevel;

    private final int attackSpeed;

    private final int attackLevel;

    private final int strengthLevel;

    private final int defenceLevel;

    private final int rangeLevel;

    private final int magicLevel;

    private final int stab;

    private final int slash;

    private final int crush;

    private final int range;

    private final int magic;

    private final int stabDef;

    private final int slashDef;

    private final int crushDef;

    private final int rangeDef;

    private final int magicDef;

    private final int bonusAttack;

    private final int bonusStrength;

    private final int bonusRangeStrength;

    private final int bonusMagicDamage;

    private final boolean poisonImmune;

    private final boolean venomImmune;

    private final boolean cannonImmune;

    private final boolean thrallImmune;

    private final boolean demon;

    private final boolean dragon;

    private final boolean fiery;

    private final boolean kalphite;

    private final boolean leafy;

    private final boolean shade;

    private final boolean spectral;

    private final boolean undead;

    private final boolean vampyre1;

    private final boolean vampyre2;

    private final boolean vampyre3;

    private final boolean xerician;

    private final int respawn;

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NPCStats))
            return false;
        NPCStats other = (NPCStats)o;
        if (getHitpoints() != other.getHitpoints())
            return false;
        if (getCombatLevel() != other.getCombatLevel())
            return false;
        if (getSlayerLevel() != other.getSlayerLevel())
            return false;
        if (getAttackSpeed() != other.getAttackSpeed())
            return false;
        if (getAttackLevel() != other.getAttackLevel())
            return false;
        if (getStrengthLevel() != other.getStrengthLevel())
            return false;
        if (getDefenceLevel() != other.getDefenceLevel())
            return false;
        if (getRangeLevel() != other.getRangeLevel())
            return false;
        if (getMagicLevel() != other.getMagicLevel())
            return false;
        if (getStab() != other.getStab())
            return false;
        if (getSlash() != other.getSlash())
            return false;
        if (getCrush() != other.getCrush())
            return false;
        if (getRange() != other.getRange())
            return false;
        if (getMagic() != other.getMagic())
            return false;
        if (getStabDef() != other.getStabDef())
            return false;
        if (getSlashDef() != other.getSlashDef())
            return false;
        if (getCrushDef() != other.getCrushDef())
            return false;
        if (getRangeDef() != other.getRangeDef())
            return false;
        if (getMagicDef() != other.getMagicDef())
            return false;
        if (getBonusAttack() != other.getBonusAttack())
            return false;
        if (getBonusStrength() != other.getBonusStrength())
            return false;
        if (getBonusRangeStrength() != other.getBonusRangeStrength())
            return false;
        if (getBonusMagicDamage() != other.getBonusMagicDamage())
            return false;
        if (isPoisonImmune() != other.isPoisonImmune())
            return false;
        if (isVenomImmune() != other.isVenomImmune())
            return false;
        if (isCannonImmune() != other.isCannonImmune())
            return false;
        if (isThrallImmune() != other.isThrallImmune())
            return false;
        if (isDemon() != other.isDemon())
            return false;
        if (isDragon() != other.isDragon())
            return false;
        if (isFiery() != other.isFiery())
            return false;
        if (isKalphite() != other.isKalphite())
            return false;
        if (isLeafy() != other.isLeafy())
            return false;
        if (isShade() != other.isShade())
            return false;
        if (isSpectral() != other.isSpectral())
            return false;
        if (isUndead() != other.isUndead())
            return false;
        if (isVampyre1() != other.isVampyre1())
            return false;
        if (isVampyre2() != other.isVampyre2())
            return false;
        if (isVampyre3() != other.isVampyre3())
            return false;
        if (isXerician() != other.isXerician())
            return false;
        if (getRespawn() != other.getRespawn())
            return false;
        Object this$name = getName(), other$name = other.getName();
        if ((this$name == null) ? (other$name != null) : !this$name.equals(other$name))
            return false;
        Object this$wiki = getWiki(), other$wiki = other.getWiki();
        return !((this$wiki == null) ? (other$wiki != null) : !this$wiki.equals(other$wiki));
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + getHitpoints();
        result = result * 59 + getCombatLevel();
        result = result * 59 + getSlayerLevel();
        result = result * 59 + getAttackSpeed();
        result = result * 59 + getAttackLevel();
        result = result * 59 + getStrengthLevel();
        result = result * 59 + getDefenceLevel();
        result = result * 59 + getRangeLevel();
        result = result * 59 + getMagicLevel();
        result = result * 59 + getStab();
        result = result * 59 + getSlash();
        result = result * 59 + getCrush();
        result = result * 59 + getRange();
        result = result * 59 + getMagic();
        result = result * 59 + getStabDef();
        result = result * 59 + getSlashDef();
        result = result * 59 + getCrushDef();
        result = result * 59 + getRangeDef();
        result = result * 59 + getMagicDef();
        result = result * 59 + getBonusAttack();
        result = result * 59 + getBonusStrength();
        result = result * 59 + getBonusRangeStrength();
        result = result * 59 + getBonusMagicDamage();
        result = result * 59 + (isPoisonImmune() ? 79 : 97);
        result = result * 59 + (isVenomImmune() ? 79 : 97);
        result = result * 59 + (isCannonImmune() ? 79 : 97);
        result = result * 59 + (isThrallImmune() ? 79 : 97);
        result = result * 59 + (isDemon() ? 79 : 97);
        result = result * 59 + (isDragon() ? 79 : 97);
        result = result * 59 + (isFiery() ? 79 : 97);
        result = result * 59 + (isKalphite() ? 79 : 97);
        result = result * 59 + (isLeafy() ? 79 : 97);
        result = result * 59 + (isShade() ? 79 : 97);
        result = result * 59 + (isSpectral() ? 79 : 97);
        result = result * 59 + (isUndead() ? 79 : 97);
        result = result * 59 + (isVampyre1() ? 79 : 97);
        result = result * 59 + (isVampyre2() ? 79 : 97);
        result = result * 59 + (isVampyre3() ? 79 : 97);
        result = result * 59 + (isXerician() ? 79 : 97);
        result = result * 59 + getRespawn();
        Object $name = getName();
        result = result * 59 + (($name == null) ? 43 : $name.hashCode());
        Object $wiki = getWiki();
        return result * 59 + (($wiki == null) ? 43 : $wiki.hashCode());
    }

    public String toString() {
        return "NPCStats(name=" + getName() + ", wiki=" + getWiki() + ", hitpoints=" + getHitpoints() + ", combatLevel=" + getCombatLevel() + ", slayerLevel=" + getSlayerLevel() + ", attackSpeed=" + getAttackSpeed() + ", attackLevel=" + getAttackLevel() + ", strengthLevel=" + getStrengthLevel() + ", defenceLevel=" + getDefenceLevel() + ", rangeLevel=" + getRangeLevel() + ", magicLevel=" + getMagicLevel() + ", stab=" + getStab() + ", slash=" + getSlash() + ", crush=" + getCrush() + ", range=" + getRange() + ", magic=" + getMagic() + ", stabDef=" + getStabDef() + ", slashDef=" + getSlashDef() + ", crushDef=" + getCrushDef() + ", rangeDef=" + getRangeDef() + ", magicDef=" + getMagicDef() + ", bonusAttack=" + getBonusAttack() + ", bonusStrength=" + getBonusStrength() + ", bonusRangeStrength=" + getBonusRangeStrength() + ", bonusMagicDamage=" + getBonusMagicDamage() + ", poisonImmune=" + isPoisonImmune() + ", venomImmune=" + isVenomImmune() + ", cannonImmune=" + isCannonImmune() + ", thrallImmune=" + isThrallImmune() + ", demon=" + isDemon() + ", dragon=" + isDragon() + ", fiery=" + isFiery() + ", kalphite=" + isKalphite() + ", leafy=" + isLeafy() + ", shade=" + isShade() + ", spectral=" + isSpectral() + ", undead=" + isUndead() + ", vampyre1=" + isVampyre1() + ", vampyre2=" + isVampyre2() + ", vampyre3=" + isVampyre3() + ", xerician=" + isXerician() + ", respawn=" + getRespawn() + ")";
    }

    NPCStats(String name, String wiki, int hitpoints, int combatLevel, int slayerLevel, int attackSpeed, int attackLevel, int strengthLevel, int defenceLevel, int rangeLevel, int magicLevel, int stab, int slash, int crush, int range, int magic, int stabDef, int slashDef, int crushDef, int rangeDef, int magicDef, int bonusAttack, int bonusStrength, int bonusRangeStrength, int bonusMagicDamage, boolean poisonImmune, boolean venomImmune, boolean cannonImmune, boolean thrallImmune, boolean demon, boolean dragon, boolean fiery, boolean kalphite, boolean leafy, boolean shade, boolean spectral, boolean undead, boolean vampyre1, boolean vampyre2, boolean vampyre3, boolean xerician, int respawn) {
        this.name = name;
        this.wiki = wiki;
        this.hitpoints = hitpoints;
        this.combatLevel = combatLevel;
        this.slayerLevel = slayerLevel;
        this.attackSpeed = attackSpeed;
        this.attackLevel = attackLevel;
        this.strengthLevel = strengthLevel;
        this.defenceLevel = defenceLevel;
        this.rangeLevel = rangeLevel;
        this.magicLevel = magicLevel;
        this.stab = stab;
        this.slash = slash;
        this.crush = crush;
        this.range = range;
        this.magic = magic;
        this.stabDef = stabDef;
        this.slashDef = slashDef;
        this.crushDef = crushDef;
        this.rangeDef = rangeDef;
        this.magicDef = magicDef;
        this.bonusAttack = bonusAttack;
        this.bonusStrength = bonusStrength;
        this.bonusRangeStrength = bonusRangeStrength;
        this.bonusMagicDamage = bonusMagicDamage;
        this.poisonImmune = poisonImmune;
        this.venomImmune = venomImmune;
        this.cannonImmune = cannonImmune;
        this.thrallImmune = thrallImmune;
        this.demon = demon;
        this.dragon = dragon;
        this.fiery = fiery;
        this.kalphite = kalphite;
        this.leafy = leafy;
        this.shade = shade;
        this.spectral = spectral;
        this.undead = undead;
        this.vampyre1 = vampyre1;
        this.vampyre2 = vampyre2;
        this.vampyre3 = vampyre3;
        this.xerician = xerician;
        this.respawn = respawn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;

        private String wiki;

        private int hitpoints;

        private int combatLevel;

        private int slayerLevel;

        private int attackSpeed;

        private int attackLevel;

        private int strengthLevel;

        private int defenceLevel;

        private int rangeLevel;

        private int magicLevel;

        private int stab;

        private int slash;

        private int crush;

        private int range;

        private int magic;

        private int stabDef;

        private int slashDef;

        private int crushDef;

        private int rangeDef;

        private int magicDef;

        private int bonusAttack;

        private int bonusStrength;

        private int bonusRangeStrength;

        private int bonusMagicDamage;

        private boolean poisonImmune;

        private boolean venomImmune;

        private boolean cannonImmune;

        private boolean thrallImmune;

        private boolean demon;

        private boolean dragon;

        private boolean fiery;

        private boolean kalphite;

        private boolean leafy;

        private boolean shade;

        private boolean spectral;

        private boolean undead;

        private boolean vampyre1;

        private boolean vampyre2;

        private boolean vampyre3;

        private boolean xerician;

        private int respawn;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder wiki(String wiki) {
            this.wiki = wiki;
            return this;
        }

        public Builder hitpoints(int hitpoints) {
            this.hitpoints = hitpoints;
            return this;
        }

        public Builder combatLevel(int combatLevel) {
            this.combatLevel = combatLevel;
            return this;
        }

        public Builder slayerLevel(int slayerLevel) {
            this.slayerLevel = slayerLevel;
            return this;
        }

        public Builder attackSpeed(int attackSpeed) {
            this.attackSpeed = attackSpeed;
            return this;
        }

        public Builder attackLevel(int attackLevel) {
            this.attackLevel = attackLevel;
            return this;
        }

        public Builder strengthLevel(int strengthLevel) {
            this.strengthLevel = strengthLevel;
            return this;
        }

        public Builder defenceLevel(int defenceLevel) {
            this.defenceLevel = defenceLevel;
            return this;
        }

        public Builder rangeLevel(int rangeLevel) {
            this.rangeLevel = rangeLevel;
            return this;
        }

        public Builder magicLevel(int magicLevel) {
            this.magicLevel = magicLevel;
            return this;
        }

        public Builder stab(int stab) {
            this.stab = stab;
            return this;
        }

        public Builder slash(int slash) {
            this.slash = slash;
            return this;
        }

        public Builder crush(int crush) {
            this.crush = crush;
            return this;
        }

        public Builder range(int range) {
            this.range = range;
            return this;
        }

        public Builder magic(int magic) {
            this.magic = magic;
            return this;
        }

        public Builder stabDef(int stabDef) {
            this.stabDef = stabDef;
            return this;
        }

        public Builder slashDef(int slashDef) {
            this.slashDef = slashDef;
            return this;
        }

        public Builder crushDef(int crushDef) {
            this.crushDef = crushDef;
            return this;
        }

        public Builder rangeDef(int rangeDef) {
            this.rangeDef = rangeDef;
            return this;
        }

        public Builder magicDef(int magicDef) {
            this.magicDef = magicDef;
            return this;
        }

        public Builder bonusAttack(int bonusAttack) {
            this.bonusAttack = bonusAttack;
            return this;
        }

        public Builder bonusStrength(int bonusStrength) {
            this.bonusStrength = bonusStrength;
            return this;
        }

        public Builder bonusRangeStrength(int bonusRangeStrength) {
            this.bonusRangeStrength = bonusRangeStrength;
            return this;
        }

        public Builder bonusMagicDamage(int bonusMagicDamage) {
            this.bonusMagicDamage = bonusMagicDamage;
            return this;
        }

        public Builder poisonImmune(boolean poisonImmune) {
            this.poisonImmune = poisonImmune;
            return this;
        }

        public Builder venomImmune(boolean venomImmune) {
            this.venomImmune = venomImmune;
            return this;
        }

        public Builder cannonImmune(boolean cannonImmune) {
            this.cannonImmune = cannonImmune;
            return this;
        }

        public Builder thrallImmune(boolean thrallImmune) {
            this.thrallImmune = thrallImmune;
            return this;
        }

        public Builder demon(boolean demon) {
            this.demon = demon;
            return this;
        }

        public Builder dragon(boolean dragon) {
            this.dragon = dragon;
            return this;
        }

        public Builder fiery(boolean fiery) {
            this.fiery = fiery;
            return this;
        }

        public Builder kalphite(boolean kalphite) {
            this.kalphite = kalphite;
            return this;
        }

        public Builder leafy(boolean leafy) {
            this.leafy = leafy;
            return this;
        }

        public Builder shade(boolean shade) {
            this.shade = shade;
            return this;
        }

        public Builder spectral(boolean spectral) {
            this.spectral = spectral;
            return this;
        }

        public Builder undead(boolean undead) {
            this.undead = undead;
            return this;
        }

        public Builder vampyre1(boolean vampyre1) {
            this.vampyre1 = vampyre1;
            return this;
        }

        public Builder vampyre2(boolean vampyre2) {
            this.vampyre2 = vampyre2;
            return this;
        }

        public Builder vampyre3(boolean vampyre3) {
            this.vampyre3 = vampyre3;
            return this;
        }

        public Builder xerician(boolean xerician) {
            this.xerician = xerician;
            return this;
        }

        public Builder respawn(int respawn) {
            this.respawn = respawn;
            return this;
        }

        public NPCStats build() {
            return new NPCStats(this.name, this.wiki, this.hitpoints, this.combatLevel, this.slayerLevel, this.attackSpeed, this.attackLevel, this.strengthLevel, this.defenceLevel, this.rangeLevel, this.magicLevel, this.stab, this.slash, this.crush, this.range, this.magic, this.stabDef, this.slashDef, this.crushDef, this.rangeDef, this.magicDef, this.bonusAttack, this.bonusStrength, this.bonusRangeStrength, this.bonusMagicDamage, this.poisonImmune, this.venomImmune, this.cannonImmune, this.thrallImmune, this.demon, this.dragon, this.fiery, this.kalphite, this.leafy, this.shade, this.spectral, this.undead, this.vampyre1, this.vampyre2, this.vampyre3, this.xerician, this.respawn);
        }

        public String toString() {
            return "NPCStats.Builder(name=" + this.name + ", wiki=" + this.wiki + ", hitpoints=" + this.hitpoints + ", combatLevel=" + this.combatLevel + ", slayerLevel=" + this.slayerLevel + ", attackSpeed=" + this.attackSpeed + ", attackLevel=" + this.attackLevel + ", strengthLevel=" + this.strengthLevel + ", defenceLevel=" + this.defenceLevel + ", rangeLevel=" + this.rangeLevel + ", magicLevel=" + this.magicLevel + ", stab=" + this.stab + ", slash=" + this.slash + ", crush=" + this.crush + ", range=" + this.range + ", magic=" + this.magic + ", stabDef=" + this.stabDef + ", slashDef=" + this.slashDef + ", crushDef=" + this.crushDef + ", rangeDef=" + this.rangeDef + ", magicDef=" + this.magicDef + ", bonusAttack=" + this.bonusAttack + ", bonusStrength=" + this.bonusStrength + ", bonusRangeStrength=" + this.bonusRangeStrength + ", bonusMagicDamage=" + this.bonusMagicDamage + ", poisonImmune=" + this.poisonImmune + ", venomImmune=" + this.venomImmune + ", cannonImmune=" + this.cannonImmune + ", thrallImmune=" + this.thrallImmune + ", demon=" + this.demon + ", dragon=" + this.dragon + ", fiery=" + this.fiery + ", kalphite=" + this.kalphite + ", leafy=" + this.leafy + ", shade=" + this.shade + ", spectral=" + this.spectral + ", undead=" + this.undead + ", vampyre1=" + this.vampyre1 + ", vampyre2=" + this.vampyre2 + ", vampyre3=" + this.vampyre3 + ", xerician=" + this.xerician + ", respawn=" + this.respawn + ")";
        }
    }

    public String getName() {
        return this.name;
    }

    public String getWiki() {
        return this.wiki;
    }

    public int getHitpoints() {
        return this.hitpoints;
    }

    public int getCombatLevel() {
        return this.combatLevel;
    }

    public int getSlayerLevel() {
        return this.slayerLevel;
    }

    public int getAttackSpeed() {
        return this.attackSpeed;
    }

    public int getAttackLevel() {
        return this.attackLevel;
    }

    public int getStrengthLevel() {
        return this.strengthLevel;
    }

    public int getDefenceLevel() {
        return this.defenceLevel;
    }

    public int getRangeLevel() {
        return this.rangeLevel;
    }

    public int getMagicLevel() {
        return this.magicLevel;
    }

    public int getStab() {
        return this.stab;
    }

    public int getSlash() {
        return this.slash;
    }

    public int getCrush() {
        return this.crush;
    }

    public int getRange() {
        return this.range;
    }

    public int getMagic() {
        return this.magic;
    }

    public int getStabDef() {
        return this.stabDef;
    }

    public int getSlashDef() {
        return this.slashDef;
    }

    public int getCrushDef() {
        return this.crushDef;
    }

    public int getRangeDef() {
        return this.rangeDef;
    }

    public int getMagicDef() {
        return this.magicDef;
    }

    public int getBonusAttack() {
        return this.bonusAttack;
    }

    public int getBonusStrength() {
        return this.bonusStrength;
    }

    public int getBonusRangeStrength() {
        return this.bonusRangeStrength;
    }

    public int getBonusMagicDamage() {
        return this.bonusMagicDamage;
    }

    public boolean isPoisonImmune() {
        return this.poisonImmune;
    }

    public boolean isVenomImmune() {
        return this.venomImmune;
    }

    public boolean isCannonImmune() {
        return this.cannonImmune;
    }

    public boolean isThrallImmune() {
        return this.thrallImmune;
    }

    public boolean isDemon() {
        return this.demon;
    }

    public boolean isDragon() {
        return this.dragon;
    }

    public boolean isFiery() {
        return this.fiery;
    }

    public boolean isKalphite() {
        return this.kalphite;
    }

    public boolean isLeafy() {
        return this.leafy;
    }

    public boolean isShade() {
        return this.shade;
    }

    public boolean isSpectral() {
        return this.spectral;
    }

    public boolean isUndead() {
        return this.undead;
    }

    public boolean isVampyre1() {
        return this.vampyre1;
    }

    public boolean isVampyre2() {
        return this.vampyre2;
    }

    public boolean isVampyre3() {
        return this.vampyre3;
    }

    public boolean isXerician() {
        return this.xerician;
    }

    public int getRespawn() {
        return this.respawn;
    }

    public double calculateXpModifier() {
        double averageLevel = Math.floor(((this.attackLevel + this.strengthLevel + this.defenceLevel + this.hitpoints) / 4));
        double averageDefBonus = Math.floor(((this.stabDef + this.slashDef + this.crushDef) / 3));
        return 1.0D + Math.floor(averageLevel * (averageDefBonus + this.bonusStrength + this.bonusAttack) / 5120.0D) / 40.0D;
    }

    public static final TypeAdapter<NPCStats> NPC_STATS_TYPE_ADAPTER = new TypeAdapter<NPCStats>() {
        public void write(JsonWriter out, NPCStats value) {
            throw new UnsupportedOperationException("Not supported");
        }

        public NPCStats read(JsonReader in) throws IOException {
            in.beginObject();
            NPCStats.Builder builder = NPCStats.builder();
            in.skipValue();
            builder.name(in.nextString());
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "wiki":
                        builder.wiki(in.nextString());
                    case "hitpoints":
                        builder.hitpoints(in.nextInt());
                    case "combatLevel":
                        builder.combatLevel(in.nextInt());
                    case "slayerLevel":
                        builder.slayerLevel(in.nextInt());
                    case "attackSpeed":
                        builder.attackSpeed(in.nextInt());
                    case "attackLevel":
                        builder.attackLevel(in.nextInt());
                    case "strengthLevel":
                        builder.strengthLevel(in.nextInt());
                    case "defenceLevel":
                        builder.defenceLevel(in.nextInt());
                    case "rangeLevel":
                        builder.rangeLevel(in.nextInt());
                    case "magicLevel":
                        builder.magicLevel(in.nextInt());
                    case "stab":
                        builder.stab(in.nextInt());
                    case "slash":
                        builder.slash(in.nextInt());
                    case "crush":
                        builder.crush(in.nextInt());
                    case "range":
                        builder.range(in.nextInt());
                    case "magic":
                        builder.magic(in.nextInt());
                    case "stabDef":
                        builder.stabDef(in.nextInt());
                    case "slashDef":
                        builder.slashDef(in.nextInt());
                    case "crushDef":
                        builder.crushDef(in.nextInt());
                    case "rangeDef":
                        builder.rangeDef(in.nextInt());
                    case "magicDef":
                        builder.magicDef(in.nextInt());
                    case "bonusAttack":
                        builder.bonusAttack(in.nextInt());
                    case "bonusStrength":
                        builder.bonusStrength(in.nextInt());
                    case "bonusRangeStrength":
                        builder.bonusRangeStrength(in.nextInt());
                    case "bonusMagicDamage":
                        builder.bonusMagicDamage(in.nextInt());
                    case "poisonImmune":
                        builder.poisonImmune(in.nextBoolean());
                    case "venomImmune":
                        builder.venomImmune(in.nextBoolean());
                    case "cannonImmune":
                        builder.cannonImmune(in.nextBoolean());
                    case "thrallImmune":
                        builder.thrallImmune(in.nextBoolean());
                    case "demon":
                        builder.demon(in.nextBoolean());
                    case "dragon":
                        builder.dragon(in.nextBoolean());
                    case "fiery":
                        builder.fiery(in.nextBoolean());
                    case "kalphite":
                        builder.kalphite(in.nextBoolean());
                    case "leafy":
                        builder.leafy(in.nextBoolean());
                    case "shade":
                        builder.shade(in.nextBoolean());
                    case "spectral":
                        builder.spectral(in.nextBoolean());
                    case "undead":
                        builder.undead(in.nextBoolean());
                    case "vampyre1":
                        builder.vampyre1(in.nextBoolean());
                    case "vampyre2":
                        builder.vampyre2(in.nextBoolean());
                    case "vampyre3":
                        builder.vampyre3(in.nextBoolean());
                    case "xerician":
                        builder.xerician(in.nextBoolean());
                    case "respawn":
                        builder.respawn(in.nextInt());
                }
            }
            in.endObject();
            return builder.build();
        }
    };
}
