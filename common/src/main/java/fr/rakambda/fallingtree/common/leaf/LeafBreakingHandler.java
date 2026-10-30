package fr.rakambda.fallingtree.common.leaf;

import fr.rakambda.fallingtree.common.FallingTreeCommon;
import fr.rakambda.fallingtree.common.wrapper.DirectionCompat;
import fr.rakambda.fallingtree.common.wrapper.IBlockPos;
import fr.rakambda.fallingtree.common.wrapper.IBlockState;
import fr.rakambda.fallingtree.common.wrapper.IServerLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class LeafBreakingHandler{
	private final Set<LeafBreakingSchedule> scheduledLeavesBreaking = ConcurrentHashMap.newKeySet();
	
	@NonNull
	private final FallingTreeCommon<?> mod;
	
	public void onServerTick(){
		var leavesBreak = scheduledLeavesBreaking.iterator();
		while(leavesBreak.hasNext()){
			var leafBreakingSchedule = leavesBreak.next();
			
			if(leafBreakingSchedule.getRemainingTicks() > 0){
				leafBreakingSchedule.tick();
				continue;
			}
			
			var level = leafBreakingSchedule.getLevel();
			var blockPos = leafBreakingSchedule.getBlockPos();
			
			// Check before touching the chunk: getChunk would load it if it isn't loaded
			if(level.hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)){
				var state = level.getBlockState(leafBreakingSchedule.getBlockPos());
				state.tick(level, leafBreakingSchedule.getBlockPos(), level.getRandom());
				if(state.isRandomlyTicking()){
					state.randomTick(level, leafBreakingSchedule.getBlockPos(), level.getRandom());
				}
			}
			
			leavesBreak.remove();
		}
	}
	
	public void onBlockUpdate(@NonNull IServerLevel level, @NonNull IBlockPos eventPos, @NonNull IBlockState eventState, Set<DirectionCompat> directions){
		if(!mod.getConfiguration().getTrees().isLeavesBreaking()){
			return;
		}
		if(!level.isServer()){
			return;
		}
		
		var eventBlock = eventState.getBlock();
		if(!eventBlock.isAir()){
			return;
		}
		
		for(var direction : directions){
			var neighborPos = eventPos.relative(direction);
			if(level.hasChunk(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)){
				var neighborState = level.getBlockState(neighborPos);
				if(mod.isLeafBlock(neighborState.getBlock())){
					addSchedule(new LeafBreakingSchedule(level, neighborPos, 4));
				}
			}
		}
	}
	
	public void addSchedule(@NonNull LeafBreakingSchedule schedule){
		scheduledLeavesBreaking.add(schedule);
	}
	
	public void onWorldUnload(@NonNull IServerLevel level){
		scheduledLeavesBreaking.removeIf(leafBreakingSchedule -> Objects.equals(level.getRaw(), leafBreakingSchedule.getLevel().getRaw()));
	}
}
